package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.embedded.impl.IdentifierUtils;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
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

	public static final Function<ApplicationUser, String> GET_EMAIL = input -> IdentifierUtils.toLowerCase(StringUtils.defaultString(input.getEmailAddress()));

	public static final Function<ApplicationUser, String> GET_FULL_NAME = input -> IdentifierUtils.toLowerCase(StringUtils.defaultString(input.getDisplayName()));

	public static final Function<ApplicationUser, String> GET_USER_NAME = input -> IdentifierUtils.toLowerCase(StringUtils.defaultString(input.getName()));

	public static ImmutableListMultimap<String, ApplicationUser> indexIgnoringNullsOrEmptyStrings(
			Collection<ApplicationUser> values, Function<ApplicationUser, String> function) {
		Preconditions.checkNotNull(values, "values");
		Preconditions.checkNotNull(function, "function");

		final ImmutableListMultimap.Builder<String, ApplicationUser> listBuilder = ImmutableListMultimap.builder();
		for(ApplicationUser value : values) {
			final String functionResult = function.apply(value);
			if (StringUtils.isNotEmpty(functionResult)) {
				listBuilder.put(functionResult, value);
			}
		}
		return listBuilder.build();
	}

	protected final Multimap<String, ApplicationUser> usersByEmail, usersByFullName, usersByUserName;

	public CachingUserMapper(Collection<ApplicationUser> users) {
		this.usersByEmail = createUsersByEmailMap(users);
		this.usersByFullName = createUsersByFullNameMap(users);
		this.usersByUserName = createUsersByUserName(users);
	}

	private Multimap<String, ApplicationUser> createUsersByUserName(Collection<ApplicationUser> users) {
		return indexIgnoringNullsOrEmptyStrings(users, GET_USER_NAME);
	}

	private Multimap<String, ApplicationUser> createUsersByFullNameMap(Collection<ApplicationUser> users) {
		return indexIgnoringNullsOrEmptyStrings(users, GET_FULL_NAME);
	}

	private Multimap<String, ApplicationUser> createUsersByEmailMap(Collection<ApplicationUser> users) {
		return indexIgnoringNullsOrEmptyStrings(users, GET_EMAIL);
	}

	@Nullable
	protected Multimap<String, ApplicationUser> getUsersByEmail(UserBean userBean, Multimap<String, ApplicationUser> usersInScope) {
        final String trimmedEmail = StringUtils.trimToNull(userBean.getEmail());
		if (trimmedEmail != null) {
            final String emailAddress = IdentifierUtils.toLowerCase(trimmedEmail);
            return Multimaps.index(usersInScope.get(emailAddress), GET_FULL_NAME);
		}
		return null;
	}

	@Nullable
    protected Multimap<String, ApplicationUser> getUsersByFullName(UserBean userBean, Multimap<String, ApplicationUser> usersInScope) {
        final String trimmedFullName = StringUtils.trimToNull(userBean.getFullName());
		if (trimmedFullName != null) {
            final String fullName = IdentifierUtils.toLowerCase(trimmedFullName);
            return Multimaps.index(usersInScope.get(fullName), GET_USER_NAME);
		}
		return null;
	}

	@Nonnull
    protected Collection<ApplicationUser> getUsersByUserName(UserBean userBean, Multimap<String, ApplicationUser> usersInScope) {
        final String trimmedName = StringUtils.trimToNull(userBean.getUserName());
		if (trimmedName != null) {
            return usersInScope.get(IdentifierUtils.toLowerCase(trimmedName));
		}
		return Collections.emptyList();
	}

	public ApplicationUser mapUser(UserBean userBean) {
		if (userBean == null) {
			return null;
		}

		Multimap<String, ApplicationUser> usersInScope = usersByEmail;
		Multimap<String, ApplicationUser> matchedUsers = getUsersByEmail(userBean, usersInScope);
		if (matchedUsers != null && matchedUsers.size() == 1) {
			final ApplicationUser user = matchedUsers.values().iterator().next();
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
			final ApplicationUser user = matchedUsers.values().iterator().next();
			log.debug(String.format(
					"Mapped remote user by full name: '%s' and email: '%s' to local user with user name: '%s'",
					userBean.getUserName(), userBean.getEmail(), user.getName()));
			return user;
		}

		usersInScope = matchedUsers != null && !matchedUsers.isEmpty() ? matchedUsers : usersByUserName;

		// finally try username
		Collection<ApplicationUser> finalMatch = getUsersByUserName(userBean, usersInScope);
		if (finalMatch.size() == 1) {
			final ApplicationUser user = finalMatch.iterator().next();
			log.debug(String.format(
					"Mapped remote user by user name: '%s' and email: '%s' to local user with user name: '%s'",
					userBean.getUserName(), userBean.getEmail(), user.getName()));
			return user;
		}

		log.debug(String.format(
				"Could not find a local user for remote user with user name: '%s' and email: '%s' returning no user",
				userBean.getUserName(), userBean.getEmail()));
		return null;
	}
}
