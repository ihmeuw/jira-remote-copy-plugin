package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.CachingUserMapper;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.VersionBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.AffectedVersionsSystemField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v1.4
 */
public class AffectedVersionsFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    private final VersionManager versionManager;

    public AffectedVersionsFieldMapper(VersionManager versionManager, FieldManager fieldManager, DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.AFFECTED_VERSIONS), defaultFieldValuesManager);
        this.versionManager = versionManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return AffectedVersionsSystemField.class;
    }

	@Override
	public void populateInputParams(CachingUserMapper userMapper, IssueInputParameters inputParameters, CopyIssueBean copyIssueBean,
			FieldLayoutItem fieldLayoutItem, Project project, IssueType issueType) {
		MappingResult mappingResult = getMappingResult(userMapper, copyIssueBean, project);
		if (!mappingResult.hasOneValidValue() && fieldLayoutItem.isRequired()) {
			String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), getFieldId(), issueType.getName());
			if (defaultFieldValue != null) {
				inputParameters.getActionParameters().put(getFieldId(), defaultFieldValue);
			}
		} else {
			populateCurrentValue(inputParameters, copyIssueBean, fieldLayoutItem, project);
		}
	}

    public void populateCurrentValue(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
    {
        final List<VersionBean> affectedVersions = makeSureNotNull(bean.getAffectedVersions());
        final List<Long> affectedVersionIds = new ArrayList<Long>();
        for (VersionBean affectedVersion : affectedVersions)
        {
            final Long affectedVersionId = findVersion(affectedVersion.getName(), project.getId());
            if (affectedVersionId != null)
            {
                affectedVersionIds.add(affectedVersionId);
            }
        }

		if (affectedVersionIds.size() > 0) {
			final Long[] ids = new Long[affectedVersionIds.size()];
			affectedVersionIds.toArray(ids);
			inputParameters.setAffectedVersionIds(ids);
		}
    }

    public boolean userHasRequiredPermission(final Project project, final ApplicationUser user)
    {
        return true;
    }

    public MappingResult getMappingResult(final CachingUserMapper userMapper, final CopyIssueBean bean, final Project project)
    {
        List<VersionBean> affectedVersions = makeSureNotNull(bean.getAffectedVersions());
        List<String> unmappedValues = new ArrayList<String>();
        if (affectedVersions.isEmpty())
        {
           return new MappingResult(unmappedValues, false, true, hasDefaultValue(project, bean));
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
        return new MappingResult(unmappedValues, hasValidValue, false, hasDefaultValue(project, bean));
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
