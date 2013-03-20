package com.atlassian.cpji.fields;

import com.atlassian.cpji.fields.system.SystemFieldIssueCreationFieldMapper;
import com.atlassian.jira.plugin.AbstractJiraModuleDescriptor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.module.ModuleFactory;

/**
 * @since v3.0
 */
public class SystemFieldMapperModuleDescriptor extends AbstractJiraModuleDescriptor<SystemFieldIssueCreationFieldMapper> implements
		StateAware {

	protected SystemFieldMapperModuleDescriptor(JiraAuthenticationContext authenticationContext, ModuleFactory moduleFactory) {
		super(authenticationContext, moduleFactory);
	}

}
