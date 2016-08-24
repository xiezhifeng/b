package com.atlassian.confluence.extra.jira.xwork;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.extra.jira.ConfluenceJiraPluginSettingManager;
import com.atlassian.confluence.extra.jira.JiraCacheManager;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;

public class CacheSettingsAction extends ConfluenceActionSupport
{
    private static final Integer JIM_CACHE_TIME = Integer.parseInt(System.getProperty("confluence.jim.cache.time", "5"));
    private ConfluenceJiraPluginSettingManager confluenceJiraPluginSettingManager;
    private JiraIssuesManager jiraIssuesManager;
    private JiraCacheManager jiraCacheManager;
    private String timeOfCacheInMinutes;
    private boolean settingsUpdated;

    public String setCacheSettings()
    {
        Integer newCacheTime;
        try
        {
            newCacheTime = Integer.parseInt(timeOfCacheInMinutes);
        }
        catch (NumberFormatException nfe)
        {
            newCacheTime = JIM_CACHE_TIME;
        }
        confluenceJiraPluginSettingManager.setTimeOfCacheInMinutes(newCacheTime);
        jiraIssuesManager.initializeCache();
        jiraCacheManager.initializeCache();
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
            return String.valueOf(JIM_CACHE_TIME);
        }

        return String.valueOf(cacheInMinutes);
    }

    public void setTimeOfCacheInMinutes(String timeOfCacheInMinutes)
    {
        this.timeOfCacheInMinutes = timeOfCacheInMinutes;
    }

    public boolean isSettingsUpdated() {
        return settingsUpdated;
    }

    public void setSettingsUpdated(boolean settingsUpdated) {
        this.settingsUpdated = settingsUpdated;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setJiraCacheManager(JiraCacheManager jiraCacheManager)
    {
        this.jiraCacheManager = jiraCacheManager;
    }
}
