package com.atlassian.cpji.components.model;

/**
 * @since v3.0
 */
public class PluginVersion extends ResultWithJiraLocation<String> {

    public PluginVersion(JiraLocation jiraLocation, String result) {
        super(jiraLocation, result);
    }
}
