package com.atlassian.cpji.fields.value;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since v1.4
 */
public class DefaultFieldValuesManagerImpl implements DefaultFieldValuesManager {
    private final PluginSettingsFactory pluginSettingsFactory;
    private static final Logger log = Logger.getLogger(DefaultFieldValuesManagerImpl.class);
    public static final String CPJI_DEFAULT_KEY = "cpji.default.%s.%s";

    public DefaultFieldValuesManagerImpl(final PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
	public void persistDefaultFieldValue(final String projectKey, final String fieldId, final String issueType, final Object fieldValue)
    {
        PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
        if (fieldValue.getClass().isArray())
        {
            String[] strValues = (String[]) fieldValue;
            List<String> values = new ArrayList<>();
            Collections.addAll(values, strValues);
            settingsForProject.put(createKey(fieldId, issueType), values);
            log.debug("Persisted default value for field '" + fieldId + "' value ='" + fieldValue + "'");
        }
        else
        {
            settingsForProject.put(CPJI_DEFAULT_KEY + fieldId, fieldValue);
            log.debug("Persisted default value for field '" + fieldId + "' value ='" + fieldValue + "'");
        }
    }

    @Override
	public void clearDefaultValue(final String projectKey, final String fieldId, final String issueType)
    {
        PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
        settingsForProject.remove(createKey(fieldId, issueType));
    }

    @Override
	public boolean hasDefaultValue(final String projectKey, final String fieldId, final String issueType)
    {
        return getDefaultFieldValue(projectKey, fieldId, issueType) != null;
    }

    @Override
	public String[] getDefaultFieldValue(final String projectKey, final String fieldId, final String issueType)
    {
        PluginSettings settingsForProject = pluginSettingsFactory.createSettingsForKey(projectKey);
        List<String> valueList = (List) settingsForProject.get(createKey(fieldId, issueType));
        if (valueList != null)
        {
            String[] strings = new String[valueList.size()];
            return valueList.toArray(strings);
        }
        return null;
    }

    private String createKey(final String fieldId, final String issueType)
    {
        return String.format(CPJI_DEFAULT_KEY, fieldId, issueType);
    }
}
