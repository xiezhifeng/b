package com.atlassian.confluence.extra.jira;

public interface ConfluenceJiraPluginSettingManager
{
    /**
     * Cache setting times for Jira Issue Macro in Minutes
     * @param minutes
     */
    void setTimeOfCacheInMinutes(Integer minutes);

    /**
     * @return minutes of cache times for Jira Issue Macro.
     *
     */
    Integer getTimeOfCacheInMinutes();
}
