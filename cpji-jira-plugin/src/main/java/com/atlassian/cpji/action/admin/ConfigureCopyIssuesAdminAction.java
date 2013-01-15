package com.atlassian.cpji.action.admin;

import com.atlassian.cpji.action.AbstractCopyIssueAction;
import com.atlassian.cpji.components.CopyIssuePermissionManager;
import com.atlassian.cpji.config.CopyIssueConfigurationManager;
import com.atlassian.cpji.config.UserMappingType;
import com.atlassian.cpji.fields.FieldLayoutItemsRetriever;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderer;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.action.issue.IssueCreationHelperBean;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import webwork.action.ActionContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @since v1.4
 */
public class ConfigureCopyIssuesAdminAction extends RequiredFieldsAwareAction {
    private UserMappingType userMappingType;

    private final IssueCreationHelperBean issueCreationHelperBean;
    private final FieldLayoutItemsRetriever fieldLayoutItemsRetriever;
    private final DefaultFieldValuesManager defaultFieldValuesManager;
    private final GroupManager groupManager;
    private final CopyIssuePermissionManager copyIssuePermissionManager;
    private final CopyIssueConfigurationManager copyIssueConfigurationManager;
    private final WebResourceManager webResourceManager;

    private static final Logger log = Logger.getLogger(ConfigureCopyIssuesAdminAction.class);

    private List<String> configChanges = new ArrayList<String>();
    private Boolean executeFired = false;
    private List<String> selectedGroups;

    public ConfigureCopyIssuesAdminAction(FieldLayoutItemsRetriever fieldLayoutItemsRetriever, IssueTypeSchemeManager issueTypeSchemeManager, final IssueFactory issueFactory, final DefaultFieldValuesManager defaultFieldValuesManager, final IssueTypeSchemeManager issueTypeSchemeManager1, final IssueCreationHelperBean issueCreationHelperBean, final DefaultFieldValuesManager defaultFieldValuesManager1, final FieldLayoutItemsRetriever fieldLayoutItemsRetriever1, final GroupManager groupManager, final CopyIssuePermissionManager copyIssuePermissionManager, final CopyIssueConfigurationManager copyIssueConfigurationManager, final WebResourceManager webResourceManager) {
        super(fieldLayoutItemsRetriever, issueTypeSchemeManager, issueFactory, defaultFieldValuesManager);
        this.fieldLayoutItemsRetriever = fieldLayoutItemsRetriever;
        this.defaultFieldValuesManager = defaultFieldValuesManager;
        this.issueCreationHelperBean = issueCreationHelperBean;
        this.groupManager = groupManager;
        this.copyIssuePermissionManager = copyIssuePermissionManager;
        this.copyIssueConfigurationManager = copyIssueConfigurationManager;
        this.webResourceManager = webResourceManager;
    }


    @Override
    public String doDefault() throws Exception {
        if (isPermissionDenied()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		if (getProject() == null) {
			return reportProjectNotFound();
		}

        requireResources();

        return INPUT;
    }

	protected String reportProjectNotFound() {
		request.setAttribute("administratorContactLink", getAdministratorContactLink());
		return "projectnotfound";
	}

	protected boolean isPermissionDenied() {
        return !getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser())
                && !getPermissionManager().hasPermission(Permissions.PROJECT_ADMIN, getProject(), getLoggedInUser());
    }

    public String doExecute() throws Exception {
        if (isPermissionDenied()) {
            return PERMISSION_VIOLATION_RESULT;
        }

		if (getProject() == null) {
			return reportProjectNotFound();
		}

        if (!"POST".equals(ActionContext.getRequest().getMethod())) {
            return returnComplete("ConfigureCopyIssuesAdminAction!default.jspa?projectKey=TST");
        }

        executeFired = true;
        saveFieldValues();
        saveGroupPermission();
        saveUserMapping();

        requireResources();

        return INPUT;


    }

    private void requireResources() {
        webResourceManager.requireResource(AbstractCopyIssueAction.RESOURCES_ADMIN_JS);
    }

    @Nonnull
    public List<Group> getGroups() {
        return Lists.newArrayList(groupManager.getAllGroups());
    }

    public boolean isGroupSelected(Group group) {
        if (selectedGroups == null) {
            this.selectedGroups = copyIssuePermissionManager.getConfiguredGroups(getProjectKey());
        }

        if (selectedGroups.contains(group.getName())) {
            return true;
        }
        return false;
    }

    public String getIssueFieldHtml() {

        FieldLayoutItem layout = fieldLayoutItemsRetriever.getIssueTypeField(getProject());
        OrderableField field = layout.getOrderableField();
        field.populateFromParams(getFieldValuesHolder(), ActionContext.getParameters());
        return field.getEditHtml(layout, this, this, getIssue(), getDisplayParameters());
    }

    private void saveFieldValues() throws Exception {
        for (FieldLayoutItem fieldLayoutItem : getFieldLayoutItems()) {
            FieldScreenRenderer fieldScreenRenderer = issueCreationHelperBean.createFieldScreenRenderer(getLoggedInUser(), getIssue());
            SimpleErrorCollection simpleErrorCollection = new SimpleErrorCollection();
            OrderableField orderableField = fieldLayoutItem.getOrderableField();
            Object fieldValue = ActionContext.getParameters().get(orderableField.getId());
            if (containsValues(fieldValue)) {
                orderableField.populateFromParams(getFieldValuesHolder(), ActionContext.getParameters());
                orderableField.validateParams(this, simpleErrorCollection, getI18nHelper(), getIssue(), fieldScreenRenderer.getFieldScreenRenderLayoutItem(orderableField));
                if (!simpleErrorCollection.hasAnyErrors()) {
                    configChanges.add(getI18nHelper().getText("cpji.config.default.value", orderableField.getName()));
                    defaultFieldValuesManager.persistDefaultFieldValue(getProject().getKey(), orderableField.getId(), getIssueTypeObject().getName(), fieldValue);
                } else {
                    getFieldValuesHolder().remove(orderableField.getId());
                    addErrorMessages(simpleErrorCollection.getErrorMessages());
                    addErrors(simpleErrorCollection.getErrors());
                    log.info("Value for field '" + orderableField.getId() + "' is invalid!" + simpleErrorCollection);
                }
            } else {
                if (defaultFieldValuesManager.hasDefaultValue(getProject().getKey(), orderableField.getId(), getIssueTypeObject().getName())) {
                    configChanges.add(getI18nHelper().getText("cpji.config.default.empty.value", orderableField.getName()));
                    defaultFieldValuesManager.clearDefaultValue(getProject().getKey(), orderableField.getId(), getIssueTypeObject().getName());
                }
            }
        }
    }


    public boolean hasConfigChanges() {
        return !configChanges.isEmpty();
    }

    public List<String> getConfigChanges() {
        return configChanges;
    }

    private void saveUserMapping() {
        UserMappingType existingUserMapping = copyIssueConfigurationManager.getUserMappingType(getProject());
        if (!existingUserMapping.equals(userMappingType)) {
            copyIssueConfigurationManager.setUserMapping(userMappingType, getProject());
            configChanges.add(getI18nHelper().getText("cpji.config.user.mapping"));
        }
    }

    private void saveGroupPermission() {
        final ImmutableList<String> configuredGroups = ImmutableList.copyOf(
                copyIssuePermissionManager.getConfiguredGroups(getProjectKey()));
        final Object rawGroups = ActionContext.getParameters().get("groups");
        final ImmutableList<String> groups = rawGroups != null ? ImmutableList.copyOf((String[]) rawGroups) : ImmutableList.<String>of();
        if (!groups.isEmpty()) {
            ImmutableList<String> selectedGroups = ImmutableList.copyOf(Iterables.filter(
                    Iterables.transform(groupManager.getAllGroups(), getGroupName()),
                    Predicates.in(groups)));

            if (configuredGroups.isEmpty() || !configuredGroups.equals(selectedGroups)) {
                copyIssuePermissionManager.restrictPermissionToGroups(getProjectKey(), selectedGroups);
                configChanges.add(getI18nHelper().getText("cpji.config.user.group"));
            }
        } else {
            if (!configuredGroups.isEmpty()) {
                copyIssuePermissionManager.clearPermissionForProject(getProjectKey());
                configChanges.add(getI18nHelper().getText("cpji.config.user.group.remove"));
            }
        }
    }

    private boolean containsValues(final Object fieldValue) {
        if (fieldValue == null) {
            return false;
        }
        if (fieldValue.getClass().isArray()) {
            String[] stringArray = (String[]) fieldValue;
            for (String s : stringArray) {
                if (StringUtils.isNotEmpty(s)) {
                    return true;
                }
            }
        } else if (fieldValue instanceof String) {
            if (StringUtils.isNotEmpty((String) fieldValue)) {
                return true;
            }
        }
        return false;
    }


    public List<UserMappingType> getUserMappingTypes() {
        return Arrays.asList(UserMappingType.values());
    }

    public UserMappingType getConfiguredUserMapping() {
        return copyIssueConfigurationManager.getUserMappingType(getProject());
    }

    public void setUserMapping(String userMapping) {
        userMappingType = UserMappingType.valueOf(userMapping);
    }

    public Boolean getExecuteFired() {
        return executeFired;
    }

    public static class GroupName implements Function<Group, String> {
        @Override
        public String apply(@Nullable Group group) {
            return group.getName();
        }
    }

    public static GroupName getGroupName() {
        return new GroupName();
    }
}
