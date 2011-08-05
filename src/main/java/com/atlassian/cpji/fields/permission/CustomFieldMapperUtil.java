package com.atlassian.cpji.fields.permission;

import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.jira.issue.fields.CustomField;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * @since v1.4
 */
public class CustomFieldMapperUtil
{

    private CustomFieldMapperUtil(){}

    public static CustomFieldBean findMatchingRemoteCustomField(final CustomField customField, List<CustomFieldBean> remoteCustomFields)
    {
        if (remoteCustomFields == null)
        {
            return null;
        }
        try
        {
            CustomFieldBean customFieldBean = Iterables.find(remoteCustomFields, new Predicate<CustomFieldBean>()
            {
                public boolean apply(final CustomFieldBean input)
                {
                    return (customFieldMatchesTypeAndName(customField, input.getCustomFieldType(), input.getCustomFieldName()));
                }
            });
            return customFieldBean;
        }
        catch (NoSuchElementException ex)
        {
            return null;
        }
    }

    public static boolean customFieldMatchesTypeAndName(CustomField customField, String type, String name)
    {
        return (customField.getName().equals(name) &&
                customField.getCustomFieldType().getClass().getCanonicalName().equals(type));
    }
}
