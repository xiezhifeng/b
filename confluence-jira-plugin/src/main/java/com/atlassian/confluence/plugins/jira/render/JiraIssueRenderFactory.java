package com.atlassian.confluence.plugins.jira.render;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.util.JiraUtil;

import java.util.Map;

public class JiraIssueRenderFactory {

    private SingleJiraIssueRender singleJiraIssueRender;

    private TableJiraIssueRender tableJiraIssueRender;

    private CountJiraIssueRender countJiraIssueRender;

    private JiraIssueRenderFactory(SingleJiraIssueRender singleJiraIssueRender, TableJiraIssueRender tableJiraIssueRender, CountJiraIssueRender countJiraIssueRender) {
        this.singleJiraIssueRender = singleJiraIssueRender;
        this.countJiraIssueRender = countJiraIssueRender;
        this.tableJiraIssueRender = tableJiraIssueRender;
    }

    public JiraIssueRender getJiraIssueRender(JiraRequestData jiraRequestData, Map<String, String> parameters)
    {
        JiraIssuesMacro.JiraIssuesType issuesType = JiraUtil.getJiraIssuesType(parameters, jiraRequestData.getRequestType(), jiraRequestData.getRequestData());

        switch (issuesType)
        {
            case SINGLE: return singleJiraIssueRender;

            case TABLE: return tableJiraIssueRender;

            case COUNT: return countJiraIssueRender;
        }

        return null;
    }
}
