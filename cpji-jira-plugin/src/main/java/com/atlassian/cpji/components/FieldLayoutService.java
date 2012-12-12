package com.atlassian.cpji.components;

import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItemImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;

/**
 * @since v3.0
 */
public class FieldLayoutService {

	private final FieldManager fieldManager;
	private final JiraAuthenticationContext authenticationContext;

	public FieldLayoutService(FieldManager fieldManager, JiraAuthenticationContext authenticationContext) {
		this.fieldManager = fieldManager;
		this.authenticationContext = authenticationContext;
	}

	public FieldLayoutItem createDefaultFieldLayoutItem(final String fieldId, final boolean required)
	{
		return new FieldLayoutItemImpl.Builder()
				.setOrderableField(fieldManager.getOrderableField(fieldId))
				.setFieldDescription(getDefaultDescription(fieldId))
				.setHidden(false)
				.setRequired(required)
				.setFieldManager(fieldManager)
				.build();
	}

	protected String getDefaultDescription(final String fieldId)
	{
		final I18nHelper i18n = authenticationContext.getI18nHelper();

		// TODO : Should get these strings on a per-user basis (i.e. get locale of user and return corresponding string).
		// TODO : At present, the default locale is used.
		if (IssueFieldConstants.ENVIRONMENT.equals(fieldId))
		{
			return i18n.getText("environment.field.description");
		}
		else if (IssueFieldConstants.TIMETRACKING.equals(fieldId))
		{
			return i18n.getText("timetracking.field.description", "*w *d *h *m", "4d, 5h 30m, 60m", "3w");
		}
		else if (IssueFieldConstants.WORKLOG.equals(fieldId))
		{
			return i18n.getText("worklog.field.description");
		}

		return null;
	}


}
