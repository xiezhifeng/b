package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
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
    JiraServerBean getJiraServer(ApplicationLink applicationLink);

    /**
     * Update Details Jira Server information
     * @param applicationLink
     */
    void updateDetailJiraServerInfor(ApplicationLink applicationLink);

    /**
     * Update primary server
     * @param applicationLink
     */
    void updatePrimaryServer(ApplicationLink applicationLink);
}
