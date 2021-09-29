package com.atlassian.cpji.fields.value;

import com.atlassian.cpji.fields.value.extractors.CachedExtractor;
import com.atlassian.cpji.fields.value.extractors.UsersByEmailExtractor;
import com.atlassian.cpji.fields.value.extractors.UsersByFullNameExtractor;
import com.atlassian.cpji.fields.value.extractors.UsersByUserNameExtractor;
import com.atlassian.cpji.rest.model.UserBean;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class CachingUserMapper {
	private static final Logger log = Logger.getLogger(CachingUserMapper.class);

	private final UserSearchService userSearchService;

	protected final CachedExtractor usersByEmail, usersByFullName, usersByUserName;

	public CachingUserMapper(UserSearchService userSearchService) {
		this.userSearchService = userSearchService;
		usersByEmail = new UsersByEmailExtractor(userSearchService);
		usersByFullName = new UsersByFullNameExtractor(userSearchService);
		usersByUserName = new UsersByUserNameExtractor(userSearchService);
	}

	/**
	 * Maps given user bean to ApplicationUser by fields in the following order:
	 * 1. Email
	 * 2. Full name (display name)
	 * 3. User name
	 *
	 * @param userBean class representing user
	 * @return mapped ApplicationUser or null if not found relevant one
	 */
	@Nullable
	public ApplicationUser mapUser(UserBean userBean) {
		if (userBean == null) {
			return null;
		}

		// filter by email
		Collection<ApplicationUser> resultSet = usersByEmail.getBy(userBean.getEmail());
		if (resultSet.size() == 1) {
			return getUserFoundByEmail(userBean, resultSet);
		}

		// filter by full name
		UsersMap processedUsers = groupUsers(resultSet, ApplicationUser::getDisplayName);
		resultSet = getBy(processedUsers, usersByFullName, userBean.getFullName());
		if (resultSet.size() == 1) {
			return getUserFoundByFullName(userBean, resultSet);
		}

		// filter by username
		processedUsers = groupUsers(resultSet, ApplicationUser::getUsername);
		resultSet = getBy(processedUsers, usersByUserName, userBean.getUserName());
		if (resultSet.size() == 1) {
			return getUserFoundByUserName(userBean, resultSet);
		}

		log.debug(String.format(
				"Could not find a local user for remote user with user name: '%s' and email: '%s' returning no user",
				userBean.getUserName(), userBean.getEmail()));
		return null;
	}

	/**
	 * Get users from the processed users collection if not empty.
	 * Otherwise, use passed cached extractor to fetch users by the given phrase/
	 *
	 * @param processedUsers collection of users to get from
	 * @param cachedExtractor extractor which implements fetching functionality
	 * @param phrase a key used for retrieving users
	 * @return fetched collection of ApplicationUser
	 */
	@Nonnull
	private Collection<ApplicationUser> getBy(@Nonnull UsersMap processedUsers,
											  @Nonnull CachedExtractor cachedExtractor,
											  @Nullable String phrase) {
		if (StringUtils.isBlank(phrase)) {
			return Collections.emptyList();
		}
		Collection<ApplicationUser> resultSet;
		if (processedUsers.isEmpty()) {
			resultSet = cachedExtractor.getBy(phrase);
		} else {
			resultSet = processedUsers.get(phrase);
		}
		return resultSet;
	}

	/**
	 * Groups users by given supplier.
	 *
	 * Eg. For supplier `ApplicationUser::getUsername` the returned map will group users by username.
	 * Username will be a key and the relevant users will be strored in collection as a value.
	 *
	 * @param users collection of users to group
	 * @param supplier
	 * @return map of users
	 */
	@Nonnull
	private UsersMap groupUsers(Collection<ApplicationUser> users, Function<ApplicationUser, String> supplier) {
		UsersMap processedUsers = new UsersMap();
		users.forEach(user -> addToCollection(processedUsers, supplier.apply(user), user));
		return processedUsers;
	}

	private void addToCollection(Map<String, Collection<ApplicationUser>> collection, String key, ApplicationUser user) {
		collection.computeIfAbsent(key, y -> new ArrayList<>());
		collection.get(key).add(user);
	}

	@Nonnull
	private ApplicationUser getUserFoundByEmail(UserBean userBean, Collection<ApplicationUser> resultSet) {
		ApplicationUser user = requireNonNull(resultSet.iterator().next());
		log.debug(String.format(
				"Mapped remote user by email: '%s' and email: '%s' to local user with user name: '%s'",
				userBean.getUserName(), userBean.getEmail(), user.getName()));
		return user;
	}

	@Nonnull
	private ApplicationUser getUserFoundByFullName(UserBean userBean, Collection<ApplicationUser> resultSet) {
		ApplicationUser user = requireNonNull(resultSet.iterator().next());
		log.debug(String.format(
				"Mapped remote user by full name: '%s' and email: '%s' to local user with user name: '%s'",
				userBean.getUserName(), userBean.getEmail(), user.getName()));
		return user;
	}

	@Nonnull
	private ApplicationUser getUserFoundByUserName(UserBean userBean, Collection<ApplicationUser> resultSet) {
		ApplicationUser user = requireNonNull(resultSet.iterator().next());
		log.debug(String.format(
				"Mapped remote user by user name: '%s' and email: '%s' to local user with user name: '%s'",
				userBean.getUserName(), userBean.getEmail(), user.getName()));
		return user;
	}
}
