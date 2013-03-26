package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.IssueCreationFieldMapper;
import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.SecurityLevelSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.issue.security.IssueSecuritySchemeManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.atlassian.cpji.fields.FieldMapperFactory.getOrderableField;

/**
 * @since v1.4
 */
public class IssueSecurityFieldMapper extends AbstractSystemFieldMapper implements IssueCreationFieldMapper {
    private final IssueSecuritySchemeManager issueSecuritySchemeManager;
    private final IssueSecurityLevelManager issueSecurityLevelManager;
    private final PermissionManager permissionManager;

    public IssueSecurityFieldMapper(IssueSecuritySchemeManager issueSecuritySchemeManager, 
			IssueSecurityLevelManager issueSecurityLevelManager, final PermissionManager permissionManager, 
			final FieldManager fieldManager, final DefaultFieldValuesManager defaultFieldValuesManager)
    {
        super(getOrderableField(fieldManager, IssueFieldConstants.SECURITY), defaultFieldValuesManager);
        this.issueSecuritySchemeManager = issueSecuritySchemeManager;
        this.issueSecurityLevelManager = issueSecurityLevelManager;
        this.permissionManager = permissionManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return SecurityLevelSystemField.class;
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
        final String issueSecurityLevel = bean.getIssueSecurityLevel();
        if (StringUtils.isNotEmpty(issueSecurityLevel))
        {
            final Long issueSecurityLevelId = findIssueSecurityLevel(issueSecurityLevel, project);
            if (issueSecurityLevelId != null)
            {
                inputParameters.setSecurityLevelId(issueSecurityLevelId);
            }
        }
    }

    public boolean userHasRequiredPermission(final Project project, final User user)
    {
        return permissionManager.hasPermission(Permissions.SET_ISSUE_SECURITY, project, user);
    }

    public MappingResult getMappingResult(final CopyIssueBean bean, final Project project)
    {
        List<String> unmappedValues = new ArrayList<String>();
        final String issueSecurityLevel = bean.getIssueSecurityLevel();
        if (StringUtils.isNotEmpty(issueSecurityLevel))
        {
            final Long issueSecurityLevelId = findIssueSecurityLevel(issueSecurityLevel, project);
            if (issueSecurityLevelId == null)
            {
                unmappedValues.add(issueSecurityLevel);
                return new MappingResult(unmappedValues, false, false, hasDefaultValue(project, bean));
            }
            else
            {
               return new MappingResult(Collections.<String>emptyList(), true, false, hasDefaultValue(project, bean));
            }
        }
        return new MappingResult(unmappedValues, false, true, hasDefaultValue(project, bean));
    }

    private Long findIssueSecurityLevel(final String issueSecurityLevel, final Project project)
    {
        try
        {
            List<GenericValue> schemes = issueSecuritySchemeManager.getSchemes(project.getGenericValue());
            if (schemes == null || schemes.isEmpty() || schemes.size() > 1)
            {
                return null;
            }
            GenericValue scheme = schemes.get(0);
            List<GenericValue> issueSecurityGV = issueSecurityLevelManager.getSchemeIssueSecurityLevels(scheme.getLong("id"));
            for (GenericValue security : issueSecurityGV)
            {
                String securityLevelName = security.getString("name");
                if (StringUtils.isNotEmpty(securityLevelName) && securityLevelName.equals(issueSecurityLevel))
                {
                    return security.getLong("id");
                }
            }
        }
        catch (GenericEntityException e)
        {
            throw new RuntimeException(e);
        }
        return null;
    }
}
