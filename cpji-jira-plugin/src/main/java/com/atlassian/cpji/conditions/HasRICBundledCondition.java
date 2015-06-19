package com.atlassian.cpji.conditions;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractJiraCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.google.common.primitives.Ints;

/**
 * @since v3.0
 */
public class HasRICBundledCondition extends AbstractJiraCondition {

    private final BuildUtilsInfo buildUtilsInfo;
    private final int[] minimumJiraVersion = new int[]{6, 0, 0};

    public HasRICBundledCondition(BuildUtilsInfo buildUtilsInfo) {
        this.buildUtilsInfo = buildUtilsInfo;
    }

    @Override
    public boolean shouldDisplay(ApplicationUser user, JiraHelper jiraHelper) {
        int[] jiraVersion = buildUtilsInfo.getVersionNumbers();
        return Ints.lexicographicalComparator().compare(jiraVersion, minimumJiraVersion) >= 0;
    }
}
