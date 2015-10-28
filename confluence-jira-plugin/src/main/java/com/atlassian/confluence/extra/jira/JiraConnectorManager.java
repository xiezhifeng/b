package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.plugins.jira.JiraServerBean;

import java.util.List;

public interface JiraConnectorManager
{
    /**
     * Get list jira server from applink config
     * @return list of JiraServerBean
     */
    List<JiraServerBean> getJiraServers();

    /**
     * Get JiraServerBean by applink
     * @param applicationLink
     * @return JiraServerBean or null if applicationLink is null
     */
    JiraServerBean getJiraServer(ReadOnlyApplicationLink applicationLink);

    /**
     * Update Details Jira Server information
     * @param applicationLink
     */
    void updateDetailJiraServerInfor(ReadOnlyApplicationLink applicationLink);

    /**
     * Update primary server
     * @param applicationLink
     */
    void updatePrimaryServer(ReadOnlyApplicationLink applicationLink);
}
