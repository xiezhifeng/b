package com.atlassian.confluence.extra.jira;

import java.util.List;

import com.atlassian.confluence.plugins.jira.JiraServerBean;

import com.atlassian.applinks.api.ApplicationLink;

public interface JiraConnectorManager
{

    /**
     * Number of minutes to wait before trying new JIRA request in case JIRA is
     * responsive
     */
    public static final int DEFAULT_RETRY_TIME_OUT_IN_MINUTE = 5;

    /**
     * System property key to override DEFAULT_RETRY_TIME_OUT_IN_MINUTE
     */
    public static final String RETRY_TIME_OUT_KEY = "jira.timeout.retry";

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
     * Check whether jira server is up, the state is cache within 15 minutes
     *
     * @param applicationLink
     * @return true if the server is up, false otherwise
     */
    boolean isJiraServerUp(ApplicationLink applicationLink);

    /**
     * Report that a jira server is down, the state will be cached within 15
     * minutes
     *
     * @param applicationLink
     */
    void reportServerDown(ApplicationLink applicationLink);

    /**
     * Update Details Jira Server information
     *
     * @param applicationLink
     */
    void updateDetailJiraServerInfor(ApplicationLink applicationLink);

    /**
     * Update primary server
     * @param applicationLink
     */
    void updatePrimaryServer(ApplicationLink applicationLink);
}
