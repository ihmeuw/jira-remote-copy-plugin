package com.atlassian.cpji.components;

import com.atlassian.cpji.fields.*;
import com.atlassian.cpji.fields.custom.CustomFieldMapper;
import com.atlassian.cpji.fields.permission.CustomFieldMapperUtil;
import com.atlassian.cpji.fields.permission.CustomFieldMappingChecker;
import com.atlassian.cpji.fields.permission.SystemFieldMappingChecker;
import com.atlassian.cpji.fields.value.DefaultFieldValuesManager;
import com.atlassian.cpji.rest.model.CopyIssueBean;
import com.atlassian.cpji.rest.model.CustomFieldBean;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.ProjectSystemField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @since v3.0
 */
public class InputParametersService {

    private static final Logger log = Logger.getLogger(InputParametersService.class);

    private final FieldMapperFactory fieldMapperFactory;
    private final FieldManager fieldManager;
    private final DefaultFieldValuesManager defaultFieldValuesManager;
    private final JiraAuthenticationContext authenticationContext;
    private final IssueTypeSchemeManager issueTypeSchemeManager;

    public InputParametersService(FieldMapperFactory fieldMapperFactory, FieldManager fieldManager, DefaultFieldValuesManager defaultFieldValuesManager, JiraAuthenticationContext authenticationContext, IssueTypeSchemeManager issueTypeSchemeManager) {
        this.fieldMapperFactory = fieldMapperFactory;
        this.fieldManager = fieldManager;
        this.defaultFieldValuesManager = defaultFieldValuesManager;
        this.authenticationContext = authenticationContext;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
    }

    public Populator getFieldsPopulator(Project project, IssueType issueType, CopyIssueBean source, Map<String, FieldMapper> allSystemFieldMappers){
        return new Populator(project, issueType, allSystemFieldMappers, source, new IssueInputParametersImpl(), fieldMapperFactory, fieldManager, defaultFieldValuesManager);
    }

    public SystemFieldMappingChecker getSystemFieldMappingChecker(Project project, CopyIssueBean copyIssueBean, FieldLayout fieldLayout){
        return new SystemFieldMappingChecker(defaultFieldValuesManager, fieldMapperFactory, authenticationContext, copyIssueBean, project, fieldLayout);
    }

    public CustomFieldMappingChecker getCustomFieldMappingChecker(Project project, CopyIssueBean copyIssueBean, FieldLayout fieldLayout){
        return new CustomFieldMappingChecker(defaultFieldValuesManager, copyIssueBean, project, fieldLayout, fieldManager, fieldMapperFactory, issueTypeSchemeManager);
    }


    public static class Populator{

        private final FieldMapperFactory fieldMapperFactory;
        private final FieldManager fieldManager;
        private final DefaultFieldValuesManager defaultFieldValuesManager;

        private final Project project;
        private final IssueType issueType;
        private final Map<String, FieldMapper> allSystemFieldMappers;
        private final IssueInputParameters inputParameters;
        private final CopyIssueBean copyIssueBean;

        public Populator(Project project, IssueType issueType, Map<String, FieldMapper> allSystemFieldMappers, CopyIssueBean copyIssueBean,  IssueInputParameters inputParameters, FieldMapperFactory fieldMapperFactory, FieldManager fieldManager, DefaultFieldValuesManager defaultFieldValuesManager) {
            this.project = project;
            this.issueType = issueType;
            this.allSystemFieldMappers = allSystemFieldMappers;
            this.inputParameters = inputParameters;
            this.copyIssueBean = copyIssueBean;

            this.fieldMapperFactory = fieldMapperFactory;
            this.fieldManager = fieldManager;
            this.defaultFieldValuesManager = defaultFieldValuesManager;
        }

        public void injectInputParam(FieldLayoutItem item){
            OrderableField orderableField = item.getOrderableField();
            if (!fieldManager.isCustomField(orderableField)) {
              populateSystemField(item, orderableField);
            } else {
                populateCustomField(item, orderableField);
            }
        }

        public void populateProjectSystemField(){
            IssueCreationFieldMapper projectFieldMapper = fieldMapperFactory.getIssueCreationFieldMapper(ProjectSystemField.class);
            projectFieldMapper.populateInputParameters(inputParameters, copyIssueBean, null, project);
        }

        private void populateCustomField(FieldLayoutItem item, OrderableField orderableField) {
            CustomField customField = fieldManager.getCustomField(orderableField.getId());
            CustomFieldMapper customFieldMapper = fieldMapperFactory.getCustomFieldMapper().get(customField.getCustomFieldType().getClass().getCanonicalName());
            if (customFieldMapper != null) {
                CustomFieldBean matchingRemoteCustomField = CustomFieldMapperUtil.findMatchingRemoteCustomField(customField, copyIssueBean.getCustomFields());
                if (matchingRemoteCustomField != null) {
                    CustomFieldMappingResult customFieldMappingResult = customFieldMapper.getMappingResult(matchingRemoteCustomField, customField, project, issueType);
                    if (!customFieldMappingResult.hasOneValidValue() && item.isRequired()) {
                        populateDefaultValue(orderableField);
                    } else {
                        customFieldMapper.populateInputParameters(inputParameters, customFieldMappingResult, customField, project, issueType);
                    }
                } else if (item.isRequired()) {
                    populateDefaultValue(orderableField);
                }
            } else {
                log.warn("No support yet for custom field type '" + customField.getCustomFieldType().getClass().getCanonicalName() + "'");
            }
        }

        private void populateDefaultValue(OrderableField orderableField) {
            String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), orderableField.getId(), issueType.getName());
            if (defaultFieldValue != null) {
                inputParameters.addCustomFieldValue(orderableField.getId(), defaultFieldValue);
            }
        }

        private void populateSystemField(FieldLayoutItem item, OrderableField orderableField) {
            IssueCreationFieldMapper fieldMapper = fieldMapperFactory.getIssueCreationFieldMapper(orderableField.getClass());
            if (fieldMapper != null) {
                MappingResult mappingResult = fieldMapper.getMappingResult(copyIssueBean, project);
                if (!mappingResult.hasOneValidValue() && item.isRequired()) {
                    String[] defaultFieldValue = defaultFieldValuesManager.getDefaultFieldValue(project.getKey(), orderableField.getId(), issueType.getName());
                    if (defaultFieldValue != null) {
                        inputParameters.getActionParameters().put(orderableField.getId(), defaultFieldValue);
                    }
                } else {
                    fieldMapper.populateInputParameters(inputParameters, copyIssueBean, item, project);
                }
            } else {
                if (!allSystemFieldMappers.containsKey(orderableField.getId())) {
                    log.warn("No support for field '" + orderableField.getName() + "'");
                }
            }
        }

        public IssueInputParameters getInputParameters(){
            return inputParameters;
        }

    }


}
