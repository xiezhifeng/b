package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.ConfluenceJiraPluginSettingManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class DefaultConfluenceJiraPluginSettingManager implements ConfluenceJiraPluginSettingManager {
    private final static String TIME_OF_CACHE_SETTING_IN_MINUTES = "com.atlassian.confluence.extra.jira.admin.cachesetting";
    private final PluginSettings settings;

    public DefaultConfluenceJiraPluginSettingManager(PluginSettingsFactory pluginSettingsFactory)
    {
        settings = pluginSettingsFactory.createGlobalSettings();
    }

    @Override
    public void setTimeOfCacheInMinutes(Integer minutes) {
        if (minutes == null)
        {
            settings.put(TIME_OF_CACHE_SETTING_IN_MINUTES, null);
        }
        else
        {
            settings.put(TIME_OF_CACHE_SETTING_IN_MINUTES, minutes.toString());
        }
    }

    @Override
    public Integer getTimeOfCacheInMinutes() {
        String minutesString = (String) settings.get(TIME_OF_CACHE_SETTING_IN_MINUTES);
        if (minutesString == null)
        {
            return null;
        }

        return Integer.valueOf(minutesString);
    }
}
