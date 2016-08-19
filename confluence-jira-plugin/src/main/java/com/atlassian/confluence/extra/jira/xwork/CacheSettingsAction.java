package com.atlassian.confluence.extra.jira.xwork;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.extra.jira.ConfluenceJiraPluginSettingManager;

public class CacheSettingsAction extends ConfluenceActionSupport
{
    private static final String DEFAULT_CACHE_IN_MINUTES = "5";
    private ConfluenceJiraPluginSettingManager confluenceJiraPluginSettingManager;
    private String timeOfCacheInMinutes;

    public String setCacheSettings()
    {
        confluenceJiraPluginSettingManager.setTimeOfCacheInMinutes(Integer.valueOf(timeOfCacheInMinutes));
        return SUCCESS;
    }

    public void setConfluenceJiraPluginSettingManager(ConfluenceJiraPluginSettingManager confluenceJiraPluginSettingManager)
    {
        this.confluenceJiraPluginSettingManager = confluenceJiraPluginSettingManager;
    }

    public String getTimeOfCacheInMinutes()
    {
        Integer cacheInMinutes = confluenceJiraPluginSettingManager.getTimeOfCacheInMinutes();

        if (cacheInMinutes == null)
        {
            return DEFAULT_CACHE_IN_MINUTES;
        }

        return cacheInMinutes.toString();
    }

    public void setTimeOfCacheInMinutes(String timeOfCacheInMinutes)
    {
        this.timeOfCacheInMinutes = timeOfCacheInMinutes;
    }

}
