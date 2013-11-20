package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.plugins.jira.JiraServerBean;

import java.util.List;

public interface JiraConnectorManager
{
    List<JiraServerBean> getJiraServers();

    JiraServerBean getJiraServer(ApplicationLink applicationLink);
}
