package com.atlassian.cpji.config;

import com.atlassian.jira.project.Project;

/**
 * @since v2.0
 */
public interface CopyIssueConfigurationManager
{
    UserMappingType getUserMappingType(final Project project);

    public void setUserMapping(UserMappingType userMapping, final Project project);
}
