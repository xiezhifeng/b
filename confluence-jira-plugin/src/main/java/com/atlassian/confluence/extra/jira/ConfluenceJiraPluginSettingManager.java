package com.atlassian.confluence.extra.jira;

public interface ConfluenceJiraPluginSettingManager
{
    /**
     * Setting JIRA Issues Macro cache timeout in minutes
     * @param minutes
     */
    void setCacheTimeoutInMinutes(Integer minutes);

    /**
     * @return Jira Issue Macro cache timeout in minutes.
     */
    Integer getCacheTimeoutInMinutes();
}
