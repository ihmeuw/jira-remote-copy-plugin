package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.VersionBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.AffectedVersionsSystemField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @since v1.4
 */
public class AffectedVersionsFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final VersionManager versionManager;

    public AffectedVersionsFieldMapper(VersionManager versionManager, Field field)
    {
        super(field);
        this.versionManager = versionManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return AffectedVersionsSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        List<VersionBean> affectedVersions = makeSureNotNull(bean.getAffectedVersions());
        List<Long> affectedVersionIds = new ArrayList<Long>();
        for (VersionBean affectedVersion : affectedVersions)
        {
            Long affectedVersionId = findVersion(affectedVersion.getName(), project.getId());
            if (affectedVersionId != null)
            {
                affectedVersionIds.add(affectedVersionId);
            }
        }
        Long[] ids = new Long[affectedVersionIds.size()];
        affectedVersionIds.toArray(ids);
        inputParameters.setAffectedVersionIds(ids);
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        List<VersionBean> affectedVersions = makeSureNotNull(bean.getAffectedVersions());
        List<String> unmappedValues = new ArrayList<String>();
        if (affectedVersions.isEmpty())
        {
           return new MappingResult(unmappedValues, false, true, defaultValueConfigured(project, bean));
        }
        boolean hasValidValue = false;
        for (VersionBean affectedVersion : affectedVersions)
        {
            Long affectedVersionId = findVersion(affectedVersion.getName(), project.getId());
            if (affectedVersionId == null)
            {
                unmappedValues.add(affectedVersion.getName());
            }
            else
            {
                hasValidValue = true;
            }
        }
        return new MappingResult(unmappedValues, hasValidValue, false, defaultValueConfigured(project, bean));
    }

    private Long findVersion(final String name, final Long projectId)
    {
        List<Version> versions = versionManager.getVersions(projectId);
        try
        {
            Version version = Iterables.find(versions, new Predicate<Version>()
            {
                public boolean apply(final Version input)
                {
                    return (name.equals(input.getName()));
                }
            });
            return version.getId();
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    private <T> List makeSureNotNull(List<T> inputList)
    {
        return (inputList == null) ? Lists.newArrayList() : inputList;
    }
}
