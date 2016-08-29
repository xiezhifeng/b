package com.atlassian.confluence.extra.jira;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public final class DefaultConfluenceJiraPluginSettingManager implements ConfluenceJiraPluginSettingManager
{
    private final static String TIME_OF_CACHE_SETTING_IN_MINUTES = "com.atlassian.confluence.extra.jira.admin.cachesetting";

    private PluginSettings settings;
    private final PluginSettingsFactory pluginSettingsFactory;

    public DefaultConfluenceJiraPluginSettingManager(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public void setCacheTimeoutInMinutes(Integer minutes) {
        if (minutes == null)
        {
            getSettings().put(TIME_OF_CACHE_SETTING_IN_MINUTES, null);
        }
        else
        {
            getSettings().put(TIME_OF_CACHE_SETTING_IN_MINUTES, minutes.toString());
        }
    }

    @Override
    public Integer getCacheTimeoutInMinutes() {
        String minutesString = (String)getSettings().get(TIME_OF_CACHE_SETTING_IN_MINUTES);
        if (minutesString == null)
        {
            return null;
        }
        return Integer.valueOf(minutesString);
    }

    private PluginSettings getSettings()
    {
        if (settings == null)
        {
            settings = pluginSettingsFactory.createGlobalSettings();
        }
        return settings;
    }
}
