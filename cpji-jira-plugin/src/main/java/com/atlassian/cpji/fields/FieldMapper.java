package com.atlassian.cpji.fields;

import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.project.Project;

/**
 * @since v1.4
 */
public interface FieldMapper
{
    boolean userHasRequiredPermission(Project project, User user);

    MappingResult getMappingResult(CachingUserMapper userMapper, CopyIssueBean bean, final Project project);

    String getFieldNameKey();

    String getFieldId();

}
