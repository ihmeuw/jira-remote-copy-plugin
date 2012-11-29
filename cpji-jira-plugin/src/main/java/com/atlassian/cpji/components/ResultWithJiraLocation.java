package com.atlassian.cpji.components;

/**
*
* @since v2.1
*/
public class ResultWithJiraLocation<T> {

    private final JiraLocation jiraLocation;
	private final T result;


    public ResultWithJiraLocation(final JiraLocation jiraLocation, final T result) {
        this.jiraLocation = jiraLocation;
        this.result = result;
    }

    public JiraLocation getJiraLocation()
    {
        return jiraLocation;
    }

    public T getResult()
    {
        return result;
    }

}
