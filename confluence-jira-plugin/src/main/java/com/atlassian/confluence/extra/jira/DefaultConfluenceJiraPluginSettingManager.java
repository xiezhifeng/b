package com.atlassian.confluence.extra.jira;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import javax.annotation.Nonnull;
import java.util.Optional;

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
    public void setCacheTimeoutInMinutes(@Nonnull Optional<Integer> minutes) {
        if (minutes.isPresent())
        {
            getSettings().put(TIME_OF_CACHE_SETTING_IN_MINUTES, minutes.get().toString());
        }
        else
        {
            getSettings().put(TIME_OF_CACHE_SETTING_IN_MINUTES, null);
        }
    }

    @Nonnull
    @Override
    public Optional<Integer> getCacheTimeoutInMinutes() {
        String minutesString = (String)getSettings().get(TIME_OF_CACHE_SETTING_IN_MINUTES);
        if (minutesString == null)
        {
            return Optional.empty();
        }
        return Optional.of(Integer.valueOf(minutesString));
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
