package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.embedded.impl.IdentifierUtils;
import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

public class CachingUserMapper {
	private static final Logger log = Logger.getLogger(CachingUserMapper.class);

	public static final Function<User, String> GET_EMAIL = new Function<User, String>() {
		@Override
		public String apply(@Nullable User input) {
			return IdentifierUtils.toLowerCase(input.getEmailAddress());
		}
	};

	public static final Function<User, String> GET_FULL_NAME = new Function<User, String>() {
		@Override
		public String apply(@Nullable User input) {
			return IdentifierUtils.toLowerCase(input.getDisplayName());
		}
	};

	public static final Function<User, String> GET_USER_NAME = new Function<User, String>() {
		@Override
		public String apply(@Nullable User input) {
			return IdentifierUtils.toLowerCase(input.getName());
		}
	};

	protected final Multimap<String, User> usersByEmail, usersByFullName, usersByUserName;

	public CachingUserMapper(Collection<User> users) {
		this.usersByEmail = createUsersByEmailMap(users);
		this.usersByFullName = createUsersByFullNameMap(users);
		this.usersByUserName = createUsersByUserName(users);
	}

	private Multimap<String, User> createUsersByUserName(Collection<User> users) {
		return Multimaps.index(users, GET_USER_NAME);
	}

	private Multimap<String, User> createUsersByFullNameMap(Collection<User> users) {
		return Multimaps.index(users, GET_FULL_NAME);
	}

	private Multimap<String, User> createUsersByEmailMap(Collection<User> users) {
		return Multimaps.index(users, GET_EMAIL);
	}


	@Nullable
	private Multimap<String, User> getUsersByEmail(UserBean userBean, Multimap<String, User> usersInScope) {
		final String emailAddress = IdentifierUtils.toLowerCase(StringUtils.trimToNull(userBean.getEmail()));
		if (emailAddress != null) {
			return Multimaps.index(usersInScope.get(emailAddress), GET_FULL_NAME);
		}
		return null;
	}

	@Nullable
	private Multimap<String, User> getUsersByFullName(UserBean userBean, Multimap<String, User> usersInScope) {
		final String fullName = IdentifierUtils.toLowerCase(StringUtils.trimToNull(userBean.getFullName()));
		if (fullName != null) {
			return Multimaps.index(usersInScope.get(fullName), GET_USER_NAME);
		}
		return null;
	}

	@Nonnull
	private Collection<User> getUsersByUserName(UserBean userBean, Multimap<String, User> usersInScope) {
		final String userName = IdentifierUtils.toLowerCase(StringUtils.trimToNull(userBean.getUserName()));
		if (userName != null) {
			return usersInScope.get(userName);
		}
		return Collections.emptyList();
	}

	public User mapUser(UserBean userBean) {
		if (userBean == null) {
			return null;
		}

		Multimap<String, User> usersInScope = usersByEmail;
		Multimap<String, User> matchedUsers = getUsersByEmail(userBean, usersInScope);
		if (matchedUsers != null && matchedUsers.size() == 1) {
			final User user = matchedUsers.values().iterator().next();
			log.debug(String.format(
					"Mapped remote user by email: '%s' and email: '%s' to local user with user name: '%s'",
					userBean.getUserName(), userBean.getEmail(),
					user.getName()));
			return user;
		}

		usersInScope = matchedUsers != null && !matchedUsers.isEmpty() ? matchedUsers : usersByFullName;

		// now limit users by full name
		matchedUsers = getUsersByFullName(userBean, usersInScope);
		if (matchedUsers != null && matchedUsers.size() == 1) {
			final User user = matchedUsers.values().iterator().next();
			log.debug(String.format(
					"Mapped remote user by full name: '%s' and email: '%s' to local user with user name: '%s'",
					userBean.getUserName(), userBean.getEmail(), user.getName()));
			return user;
		}

		usersInScope = matchedUsers != null && !matchedUsers.isEmpty() ? matchedUsers : usersByUserName;

		// finally try username
		Collection<User> finalMatch = getUsersByUserName(userBean, usersInScope);
		if (finalMatch.size() == 1) {
			final User user = finalMatch.iterator().next();
			log.debug(String.format(
					"Mapped remote user by user name: '%s' and email: '%s' to local user with user name: '%s'",
					userBean.getUserName(), userBean.getEmail(), user.getName()));
			return user;
		}

		log.warn(String.format(
				"Could not find a local user for remote user with user name: '%s' and email: '%s' returning no user",
				userBean.getUserName(), userBean.getEmail()));
		return null;
	}
}