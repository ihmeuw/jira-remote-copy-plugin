package com.atlassian.cpji.action.admin;

import com.atlassian.cpji.config.CopyIssueConfigurationManager;
import com.atlassian.cpji.config.DefaultCopyIssueConfigurationManager;
import com.atlassian.cpji.config.UserMappingType;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutStorageException;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderer;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.action.issue.IssueCreationHelperBean;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import webwork.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @since v1.4
 */
public class ConfigureCopyIssuesAdminAction extends JiraWebActionSupport implements OperationContext
{
    public static final String SECURITYBREACH = "securitybreach";
    private String projectKey;
    private List<IssueType> issueTypesForProject;
    private String selectedIssueTypeId;
    private UserMappingType userMappingType;

    private final IssueTypeSchemeManager issueTypeSchemeManager;
    private final IssueFactory issueFactory;
    private final IssueCreationHelperBean issueCreationHelperBean;
    private final DefaultFieldValuesManager defaultFieldValuesManager;
    private final FieldLayoutItemsRetriever fieldLayoutItemsRetriever;
    private final GroupManager groupManager;
    private final CopyIssuePermissionManager copyIssuePermissionManager;
    private CopyIssueConfigurationManager copyIssueConfigurationManager;

    private static final Logger log = Logger.getLogger(ConfigureCopyIssuesAdminAction.class);

    private static final ArrayList<String> unmodifiableFields = Lists.newArrayList(IssueFieldConstants.ISSUE_TYPE, IssueFieldConstants.PROJECT);
    private MutableIssue issue;
    private Map fieldValuesHolder;
    private String group;
    private List<String> configChanges = new ArrayList<String>();

    public ConfigureCopyIssuesAdminAction(
            final IssueTypeSchemeManager issueTypeSchemeManager,
            final IssueFactory issueFactory,
            final IssueCreationHelperBean issueCreationHelperBean,
            final DefaultFieldValuesManager defaultFieldValuesManager,
            final FieldLayoutItemsRetriever fieldLayoutItemsRetriever,
            final GroupManager groupManager,
            final CopyIssuePermissionManager copyIssuePermissionManager,
            final DefaultCopyIssueConfigurationManager copyIssueConfigurationManager)
    {
        this.issueTypeSchemeManager = issueTypeSchemeManager;
        this.issueFactory = issueFactory;
        this.issueCreationHelperBean = issueCreationHelperBean;
        this.defaultFieldValuesManager = defaultFieldValuesManager;
        this.fieldLayoutItemsRetriever = fieldLayoutItemsRetriever;
        this.groupManager = groupManager;
        this.copyIssuePermissionManager = copyIssuePermissionManager;
        this.copyIssueConfigurationManager = copyIssueConfigurationManager;
        fieldValuesHolder = new HashMap();
    }

    public String doExecute() throws Exception
    {
        if (!getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser()) && !getPermissionManager().hasPermission(Permissions.PROJECT_ADMIN, getProject(), getLoggedInUser()))
        {
            return SECURITYBREACH;
        }
        return SUCCESS;
    }

    public List<Group> getGroups()
    {
        return Lists.newArrayList(groupManager.getAllGroups());
    }

    public void setGroup(String group)
    {
        this.group = group;
    }

    public String getSelectedGroup()
    {
        String configuredGroup = copyIssuePermissionManager.getConfiguredGroup(projectKey);
        if (StringUtils.isEmpty(configuredGroup))
        {
            return "";
        }
        return configuredGroup;
    }

    public String doSave() throws Exception
    {
        if (!getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser()) && !getPermissionManager().hasPermission(Permissions.PROJECT_ADMIN, getProject(), getLoggedInUser()))
        {
            return SECURITYBREACH;
        }
        for (FieldLayoutItem fieldLayoutItem : getFieldLayoutItems())
        {
            FieldScreenRenderer fieldScreenRenderer = issueCreationHelperBean.createFieldScreenRenderer(getRemoteUser(), getIssue());
            SimpleErrorCollection simpleErrorCollection = new SimpleErrorCollection();
            OrderableField orderableField = fieldLayoutItem.getOrderableField();
            Object fieldValue = ActionContext.getParameters().get(orderableField.getId());
            if (containsValues(fieldValue))
            {
                orderableField.populateFromParams(fieldValuesHolder, ActionContext.getParameters());
                orderableField.validateParams(this, simpleErrorCollection, getI18nHelper(), getIssue(), fieldScreenRenderer.getFieldScreenRenderLayoutItem(orderableField));
                if (!simpleErrorCollection.hasAnyErrors())
                {
                    configChanges.add(getI18nHelper().getText("cpji.config.default.value", orderableField.getName()));
                    defaultFieldValuesManager.persistDefaultFieldValue(getProject().getKey(), orderableField.getId(), getIssueType().getName(), fieldValue);
                }
                else
                {
                    addErrorMessages(simpleErrorCollection.getErrorMessages());
                    addErrors(simpleErrorCollection.getErrors());
                    log.error("Value for field '" + orderableField.getId() + "' is invalid!" + simpleErrorCollection);
                }
            }
            else
            {
                if (defaultFieldValuesManager.hasDefaultValue(getProject().getKey(), orderableField.getId(), getIssueType().getName()))
                {
                    configChanges.add(getI18nHelper().getText("cpji.config.default.empty.value", orderableField.getName()));
                    defaultFieldValuesManager.clearDefaultValue(getProject().getKey(), orderableField.getId(), getIssueType().getName());
                }
            }
        }
        return SUCCESS;
    }

    public String doSavePermission() throws Exception
    {
        if (!getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser()) && !getPermissionManager().hasPermission(Permissions.PROJECT_ADMIN, getProject(), getLoggedInUser()))
        {
            return SECURITYBREACH;
        }
        saveGroupPermission();
        saveUserMapping();
        configChanges.add(getI18nHelper().getText("cpji.config.user.mapping"));
        return SUCCESS;
    }

    public boolean hasConfigChanges()
    {
        return !configChanges.isEmpty();
    }

    public List<String> getConfigChanges()
    {
        return configChanges;
    }

    private void saveUserMapping()
    {
        copyIssueConfigurationManager.setUserMapping(userMappingType, getProject());
    }

    private void saveGroupPermission()
    {
        if (StringUtils.isNotEmpty(group))
        {
            Group selectedGroup = Iterables.find(groupManager.getAllGroups(), new Predicate<Group>()
            {
                public boolean apply(final Group input)
                {
                    return group.equals(input.getName());
                }
            });
            copyIssuePermissionManager.restrictPermissionToGroup(projectKey, selectedGroup.getName());
        }
        else
        {
            copyIssuePermissionManager.clearPermissionForProject(projectKey);
        }
    }

    private boolean containsValues(final Object fieldValue)
    {
        if (fieldValue == null)
        {
            return false;
        }
        if (fieldValue.getClass().isArray())
        {
            String[] stringArray = (String[]) fieldValue;
            for (String s : stringArray)
            {
                if (StringUtils.isNotEmpty(s))
                {
                    return true;
                }
            }
        }
        else if (fieldValue instanceof String)
        {
            if (StringUtils.isNotEmpty((String) fieldValue))
            {
                return true;
            }
        }
        return false;
    }

    public String getHtmlForField(FieldLayoutItem fieldLayoutItem)
    {
        OrderableField orderableField = fieldLayoutItem.getOrderableField();
        Object defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(getProject().getKey(), orderableField.getId(), getIssueType().getName());
        if (ActionContext.getParameters().get(orderableField.getId()) == null)
        {
            if (defaultFieldValue != null)
            {
                Map actionParams = new HashMap();
                actionParams.put(orderableField.getId(), defaultFieldValue);
                orderableField.populateFromParams(getFieldValuesHolder(), actionParams);
            }
        }

        return orderableField.getEditHtml(fieldLayoutItem, this, this, getIssue(), getDisplayParameters());
    }

    private Map getDisplayParameters()
    {
        Map displayParameters = new HashMap();
        displayParameters.put("theme", "aui");
        return displayParameters;
    }

    private MutableIssue getIssue()
    {
        if (issue == null)
        {
            issue = issueFactory.getIssue();
        }
        issue.setProjectId(getProject().getId());
        issue.setIssueTypeId(getIssueType().getId());
        return issue;
    }


    public List<FieldLayoutItem> getFieldLayoutItems() throws FieldLayoutStorageException
    {
        Iterable<FieldLayoutItem> filter = Iterables.filter(fieldLayoutItemsRetriever.getAllVisibleFieldLayoutItems(getProject(), getIssueType()), new Predicate<FieldLayoutItem>()
        {
            public boolean apply(final FieldLayoutItem input)
            {
                return !unmodifiableFields.contains(input.getOrderableField().getId()) && input.isRequired();
            }
        });
        return Lists.newArrayList(filter);
    }

    private Project getProject()
    {
        if (StringUtils.isEmpty(projectKey))
        {
            throw new RuntimeException("Project key cannot be null!");
        }
        Project project = getProjectManager().getProjectObjByKey(projectKey);
        if (project == null)
        {
            throw new RuntimeException("Project cannot be null!");
        }
        return project;
    }

    public List<IssueType> getIssueTypesForProject()
    {
        issueTypesForProject = Lists.newArrayList(issueTypeSchemeManager.getIssueTypesForProject(getProject()));
        if (selectedIssueTypeId == null)
        {
            selectedIssueTypeId = issueTypesForProject.get(0).getId();
        }
        return issueTypesForProject;
    }

    public void setProjectKey(String projectKey)
    {
        this.projectKey = projectKey;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public String getSelectedIssueTypeId()
    {
        return selectedIssueTypeId;
    }

    public String getSelectedIssueTypeName()
    {
        List<IssueType> issueTypesForProject1 = getIssueTypesForProject();
        for (IssueType issueType : issueTypesForProject1)
        {
            if (issueType.getId().equals(selectedIssueTypeId))
            {
                return issueType.getName();
            }
        }
        return "none";
    }

    public void setSelectedIssueTypeId(final String selectedIssueTypeId)
    {
        this.selectedIssueTypeId = selectedIssueTypeId;
    }

    private IssueType getIssueType()
    {
        try
        {
            return Iterables.find(issueTypeSchemeManager.getIssueTypesForProject(getProject()), new Predicate<IssueType>()
            {
                public boolean apply(final IssueType input)
                {
                    return input.getId().equals(selectedIssueTypeId);
                }
            });
        }
        catch (NoSuchElementException ex)
        {
            throw new RuntimeException("Failed to find issue type with id '" + selectedIssueTypeId + "'");
        }
    }

    public Map getFieldValuesHolder()
    {
        return fieldValuesHolder;
    }

    public IssueOperation getIssueOperation()
    {
        return IssueOperations.EDIT_ISSUE_OPERATION;
    }

    public List<UserMappingType> getUserMappingTypes()
    {
        return Arrays.asList(UserMappingType.values());
    }

    public UserMappingType getConfiguredUserMapping()
    {
        return copyIssueConfigurationManager.getUserMappingType(getProject());
    }

    public void setUserMapping(String userMapping)
    {
        userMappingType = UserMappingType.valueOf(userMapping);
    }
}
