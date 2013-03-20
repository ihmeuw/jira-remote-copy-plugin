package com.atlassian.cpji.fields;

import com.atlassian.cpji.fields.custom.CustomFieldMapper;
import com.atlassian.jira.plugin.AbstractJiraModuleDescriptor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.module.ModuleFactory;

/**
 * @since v3.0
 */
public class CustomFieldMapperModuleDescriptor extends AbstractJiraModuleDescriptor<CustomFieldMapper> implements
		StateAware {

	protected CustomFieldMapperModuleDescriptor(JiraAuthenticationContext authenticationContext, ModuleFactory moduleFactory) {
		super(authenticationContext, moduleFactory);
	}

}
