package com.atlassian.cpji.fields.value

import com.atlassian.cpji.rest.model.UserBean
import com.atlassian.jira.bc.user.search.UserSearchService
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserManager
import com.google.common.collect.Lists
import org.junit.runner.RunWith
import org.junit.{Before, Test}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.mockito.junit.MockitoJUnitRunner
import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.mock.MockitoSugar

@RunWith(classOf[MockitoJUnitRunner])
class TestUserMappingManager extends ShouldMatchersForJUnit with MockitoSugar {

	val userManager = mock[UserManager]
	val userSearchService: UserSearchService = mock[UserSearchService]
	var userMappingManager : UserMappingManager = null

	@Before def setUp {
		userMappingManager = new UserMappingManager(userManager, userSearchService)
	}

	@Test def shouldNoCrashWhenThereAreNoUsers {
		userMappingManager.getUserMapper
      .mapUser(new UserBean("pniewiadomski", "pniewiadomski@atlassian.com", "Pawel Niewiadomski")
      ) should be (null)
	}

	@Test def shouldMatchByEmailFirst {
		val testedUser = mockUser("pn", "Pawel", "pniewiadomski@atlassian.com")

		when(userSearchService.findUsersByEmail(testedUser.getEmailAddress)) thenReturn Lists.newArrayList(testedUser)

		userMappingManager.getUserMapper
      .mapUser(
        new UserBean("pniewiadomski", testedUser.getEmailAddress, "Pawel Niewiadomski")
			) should have (
			'emailAddress ("pniewiadomski@atlassian.com"),
			'username ("pn"),
			'displayName ("Pawel")
		)
    verify(userSearchService) findUsersByEmail any()
    verify(userSearchService, never) findUsersByFullName any()
    verify(userSearchService, never) getUserByName(any(), any())
  }

	@Test def shouldMatchByFullNameIfNoEmailMatches {
    val testedUser = mockUser(
      "pniewiadomski", "Pawel Niewiadomski", "11110000b@gmail.com")

    when(userSearchService.findUsersByFullName(testedUser.getDisplayName)) thenReturn Lists.newArrayList(testedUser)

		userMappingManager.getUserMapper
      .mapUser(
        new UserBean("pniewiadomski", "pniewiadomski@atlassian.com", "Pawel Niewiadomski")
      ) should have (
			'emailAddress (testedUser.getEmailAddress),
			'username (testedUser.getUsername),
			'displayName (testedUser.getDisplayName)
		)
    verify(userSearchService) findUsersByEmail any()
    verify(userSearchService) findUsersByFullName any()
    verify(userSearchService, never) getUserByName(any(), any())
	}

	@Test def shouldMatchByUserNameIfNothingElseMatches {
		val users = Lists.newArrayList[ApplicationUser](mockUser("pniewiadomski", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
			mockUser("pn", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
			mockUser("admin", "Pawel Niewiadomski", "pawelniewiadomski@me.com"))

    val testedUser = mockUser("admin", "Pawel Niewiadomski", "pawelniewiadomski@me.com")

    when(userSearchService.getUserByName(null, testedUser.getUsername)) thenReturn testedUser

		userMappingManager.getUserMapper
      .mapUser(
        new UserBean(testedUser.getUsername, testedUser.getEmailAddress, testedUser.getDisplayName)
      ) should have (
      'emailAddress (testedUser.getEmailAddress),
      'username (testedUser.getUsername),
      'displayName (testedUser.getDisplayName)
    )

    verify(userSearchService) findUsersByEmail any()
    verify(userSearchService) findUsersByFullName any()
    verify(userSearchService) getUserByName(any(), any())
	}

  @Test def shouldMatchByUserNameIfAllMatchesMultiple {
    val testedUser = mockUser("admin", "Pawel Niewiadomski", "pawelniewiadomski@me.com")
    val users = Lists.newArrayList[ApplicationUser](
      mockUser("pniewiadomski", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
      mockUser("pn", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
      mockUser("admin", "Pawel Niewiadomski", "pawelniewiadomski@me.com")
    )

    when(userSearchService.findUsersByEmail(testedUser.getEmailAddress)) thenReturn Lists.newArrayList(users)

    userMappingManager.getUserMapper
      .mapUser(
        new UserBean(testedUser.getUsername, testedUser.getEmailAddress, testedUser.getDisplayName)
      ) should have (
      'emailAddress (testedUser.getEmailAddress),
      'username (testedUser.getUsername),
      'displayName (testedUser.getDisplayName)
    )
    verify(userSearchService) findUsersByEmail any()
    verify(userSearchService, never) findUsersByFullName any()
    verify(userSearchService, never) getUserByName(any(), any())
  }

  @Test def shouldMatchByUserNameIfFullNameMatchesMultiple {
    val testedUser = mockUser("admin", "Pawel Niewiadomski", "pawelniewiadomski@me.com")
    val users = Lists.newArrayList[ApplicationUser](
      mockUser("pniewiadomski", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
      mockUser("pn", "Pawel Niewiadomski", "pawelniewiadomski@me.com"),
      mockUser("admin", "Pawel Niewiadomski", "pawelniewiadomski@me.com")
    )

    when(userSearchService.findUsersByFullName(testedUser.getDisplayName)) thenReturn Lists.newArrayList(users)

    userMappingManager.getUserMapper
      .mapUser(
        new UserBean(testedUser.getUsername, testedUser.getEmailAddress, testedUser.getDisplayName)
      ) should have (
      'emailAddress (testedUser.getEmailAddress),
      'username (testedUser.getUsername),
      'displayName (testedUser.getDisplayName)
    )
    verify(userSearchService) findUsersByEmail any()
    verify(userSearchService) findUsersByFullName any()
    verify(userSearchService, never) getUserByName(any(), any())
  }

	def mockUser(username: String, fullName: String, email: String) : ApplicationUser = {
		val user : ApplicationUser = mock[ApplicationUser]
		when(user.getUsername).thenReturn(username)
		when(user.getDisplayName).thenReturn(fullName)
		when(user.getEmailAddress).thenReturn(email)
		when(user.isActive).thenReturn(true)
		user
	}
}
