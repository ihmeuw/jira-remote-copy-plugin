package com.atlassian.cpji.fields.value

import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.mock.MockitoSugar
import com.atlassian.jira.user.util.UserManager
import org.junit.{Test, Before}
import com.atlassian.cpji.rest.model.UserBean
import com.google.common.collect.Lists
import org.mockito.Mockito.when
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
		val users = Lists.newArrayList[User](mockUser("pniewiadomski", "Pawel Niewiadomski", "11110000b@gmail.com"),
			mockUser("pn", "Pawel", "pniewiadomski@atlassian.com"),
			mockUser("admin", "admin", "admin@localhost"))

		when(userManager.getUsers).thenReturn(users)

		userMappingManager.mapUser(new UserBean("pniewiadomski", "pniewiadomski@atlassian.com", "Pawel Niewiadomski")) should have (
			'emailAddress ("pniewiadomski@atlassian.com"),
			'name ("pn"),
			'displayName ("Pawel")
		)
	}

	@Test def shouldMatchByFullNameIfNoEmailMatches {
		val users = Lists.newArrayList[User](mockUser("pniewiadomski", "Pawel Niewiadomski", "11110000b@gmail.com"),
			mockUser("pn", "Pawel", "pawelniewiadomski@me.com"),
			mockUser("admin", "admin", "admin@localhost"))

		when(userManager.getUsers).thenReturn(users)

		userMappingManager.mapUser(new UserBean("pniewiadomski", "pniewiadomski@atlassian.com", "Pawel Niewiadomski")) should have (
			'emailAddress ("11110000b@gmail.com"),
			'name ("pniewiadomski"),
			'displayName ("Pawel Niewiadomski")
		)
	}

	@Test def shouldMatchByUserNameIfNothingElseMatches {
		val users = Lists.newArrayList[User](mockUser("pniewiadomski", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
			mockUser("pn", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
			mockUser("admin", "Pawel Niewiadomski", "pawelniewiadomski@me.com"))

		when(userManager.getUsers).thenReturn(users)

		userMappingManager.mapUser(new UserBean("admin", "pawelniewiadomski@me.com", "Pawel Niewiadomski")) should have (
			'emailAddress ("pawelniewiadomski@me.com"),
			'name ("admin"),
			'displayName ("Pawel Niewiadomski")
		)
	}

	def mockUser(username: String, fullName: String, email: String) : User = {
		val user : User = mock[User]
		when(user.getName).thenReturn(username)
		when(user.getDisplayName).thenReturn(fullName)
		when(user.getEmailAddress).thenReturn(email)
		return user
	}
}
