package com.atlassian.cpji.fields.system;

import com.atlassian.cpji.fields.MappingResult;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.SecurityLevelSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
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

/**
 * @since v1.4
 */
public class IssueSecurityFieldMapper extends AbstractFieldMapper implements SystemFieldIssueCreationFieldMapper
{
    private final IssueSecuritySchemeManager issueSecuritySchemeManager;
    private final IssueSecurityLevelManager issueSecurityLevelManager;
    private final PermissionManager permissionManager;

    public IssueSecurityFieldMapper(IssueSecuritySchemeManager issueSecuritySchemeManager, IssueSecurityLevelManager issueSecurityLevelManager, final PermissionManager permissionManager, final Field field)
    {
        super(field);
        this.issueSecuritySchemeManager = issueSecuritySchemeManager;
        this.issueSecurityLevelManager = issueSecurityLevelManager;
        this.permissionManager = permissionManager;
    }

    public Class<? extends OrderableField> getField()
    {
        return SecurityLevelSystemField.class;
    }

    public void populateInputParameters(final IssueInputParameters inputParameters, final CopyIssueBean bean, final FieldLayoutItem fieldLayoutItem, final Project project)
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
                return new MappingResult(unmappedValues, false, false);
            }
            else
            {
               return new MappingResult(Collections.<String>emptyList(), true, false);
            }
        }
        return new MappingResult(unmappedValues, false, true);
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
