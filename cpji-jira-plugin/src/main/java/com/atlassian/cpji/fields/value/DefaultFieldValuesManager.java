package com.atlassian.cpji.fields.value;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.0
 */
public interface DefaultFieldValuesManager {
	void persistDefaultFieldValue(String projectKey, String fieldId, String issueType, Object fieldValue);

	void clearDefaultValue(String projectKey, String fieldId, String issueType);

	boolean hasDefaultValue(String projectKey, String fieldId, String issueType);

	String[] getDefaultFieldValue(String projectKey, String fieldId, String issueType);
}
