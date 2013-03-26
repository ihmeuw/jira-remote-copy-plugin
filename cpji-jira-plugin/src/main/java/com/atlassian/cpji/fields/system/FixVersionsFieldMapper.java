package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.VersionBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.FixVersionsSystemField;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
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
public class FixVersionsFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    private final VersionManager versionManager;
    private final PermissionManager permissionManager;

    public FixVersionsFieldMapper(VersionManager versionManager, final PermissionManager permissionManager, 
			final FieldManager fieldManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.FIX_FOR_VERSIONS), defaultFieldValuesManager);
        this.versionManager = versionManager;
        this.permissionManager = permissionManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return FixVersionsSystemField.class;
    }

	@Override
	public void populateInputParams(IssueInputParameters inputParameters, CopyIssueBean copyIssueBean,
			FieldLayoutItem fieldLayoutItem, Project project, IssueType issueType) {
		MappingResult mappingResult = getMappingResult(copyIssueBean, project);
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
        List<VersionBean> fixVersions = makeSureNotNull(bean.getFixedForVersions());
        List<Long> fixVersionIds = new ArrayList<Long>();
        for (VersionBean fixVersion : fixVersions)
        {
            Long fixVersionId = findVersion(fixVersion.getName(), project.getId());
            if (fixVersionId != null)
            {
                fixVersionIds.add(fixVersionId);
            }
        }
        Long[] ids = new Long[fixVersionIds.size()];
        fixVersionIds.toArray(ids);
        inputParameters.setFixVersionIds(ids);
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.RESOLVE_ISSUE, project, user);
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

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        List<VersionBean> fixforVersions = makeSureNotNull(bean.getFixedForVersions());
        List<String> unmappedValues = new ArrayList<String>();
        if (fixforVersions.isEmpty())
        {
         return new MappingResult(unmappedValues, false, true, hasDefaultValue(project, bean));
        }
        boolean hasValidValue = false;
        for (VersionBean fixedForVersion : fixforVersions)
        {
            Long versionId = findVersion(fixedForVersion.getName(), project.getId());
            if (versionId == null)
            {
                unmappedValues.add(fixedForVersion.getName());
            }
            else
            {
                hasValidValue = true;
            }
        }
        return new MappingResult(unmappedValues, hasValidValue, false, hasDefaultValue(project, bean));
    }

    private <T> List makeSureNotNull(List<T> inputList)
    {
        return (inputList == null) ? Lists.newArrayList() : inputList;
    }
}
