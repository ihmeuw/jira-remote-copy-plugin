package com.atlassian.cpji.fields.value

import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.mock.MockitoSugar
import com.atlassian.jira.user.util.UserManager
import org.junit.{Test, Before}
import com.atlassian.cpji.rest.model.UserBean
import com.google.common.collect.Lists
import com.atlassian.jira.user.MockUser
import org.mockito.Mockito.{when}
import com.atlassian.crowd.embedded.api.User

@RunWith(classOf[MockitoJUnitRunner])
class TestUserMappingManager extends ShouldMatchersForJUnit with MockitoSugar {

	val userManager = mock[UserManager]
	var userMappingManager : UserMappingManager = null

	@Before def setUp {
		userMappingManager = new UserMappingManager(userManager)
	}

	@Test def shouldNoCrashWhenThereAreNoUsers {
		userMappingManager.mapUser(new UserBean("pniewiadomski", "pniewiadomski@atlassian.com", "Pawel Niewiadomski")) should be (null)
	}

	@Test def shouldMatchByEmailFirst {
		val users = Lists.newArrayList[User](new MockUser("pniewiadomski", "Pawel Niewiadomski", "11110000b@gmail.com"),
			new MockUser("pn", "Pawel", "pniewiadomski@atlassian.com"),
			new MockUser("admin"))

		when(userManager.getUsers).thenReturn(users)

		userMappingManager.mapUser(new UserBean("pniewiadomski", "pniewiadomski@atlassian.com", "Pawel Niewiadomski")) should have (
			'emailAddress ("pniewiadomski@atlassian.com"),
			'name ("pn"),
			'displayName ("Pawel")
		)
	}

	@Test def shouldMatchByFullNameIfNoEmailMatches {
		val users = Lists.newArrayList[User](new MockUser("pniewiadomski", "Pawel Niewiadomski", "11110000b@gmail.com"),
			new MockUser("pn", "Pawel", "pawelniewiadomski@me.com"),
			new MockUser("admin"))

		when(userManager.getUsers).thenReturn(users)

		userMappingManager.mapUser(new UserBean("pniewiadomski", "pniewiadomski@atlassian.com", "Pawel Niewiadomski")) should have (
			'emailAddress ("11110000b@gmail.com"),
			'name ("pniewiadomski"),
			'displayName ("Pawel Niewiadomski")
		)
	}

	@Test def shouldMatchByUserNameIfNothingElseMatches {
		val users = Lists.newArrayList[User](new MockUser("pniewiadomski", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
			new MockUser("pn", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
			new MockUser("admin", "Pawel Niewiadomski", "pawelniewiadomski@me.com"))

		when(userManager.getUsers).thenReturn(users)

		userMappingManager.mapUser(new UserBean("admin", "pawelniewiadomski@me.com", "Pawel Niewiadomski")) should have (
			'emailAddress ("pawelniewiadomski@me.com"),
			'name ("admin"),
			'displayName ("Pawel Niewiadomski")
		)
	}
}
