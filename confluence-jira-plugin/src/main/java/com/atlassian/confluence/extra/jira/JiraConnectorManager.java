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
     * @return JiraServerBean
     */
    JiraServerBean getJiraServer(ApplicationLink applicationLink);
}
