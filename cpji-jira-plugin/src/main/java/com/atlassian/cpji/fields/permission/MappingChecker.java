package com.atlassian.cpji.fields.permission;

import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.PermissionBean;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;

import java.util.List;

/**
 *
 * @since v1.4
 */
public interface MappingChecker<T extends PermissionBean>
{
    /**
     *
     * @param copyIssueBean
     * @param fieldLayoutItems
     * @return
     */
    public List<T> findUnmappedRemoteFields(CopyIssueBean copyIssueBean, Iterable<FieldLayoutItem> fieldLayoutItems);

    /**
     *
     * @param fieldId
     * @return
     */
    public T getFieldPermission(String fieldId);


}
