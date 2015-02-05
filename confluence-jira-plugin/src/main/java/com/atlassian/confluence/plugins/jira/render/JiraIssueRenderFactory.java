package com.atlassian.confluence.plugins.jira.render;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.plugins.jira.render.count.CountJiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.single.DynamicSingleJiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.single.StaticSingleJiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.table.DynamicTableJiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.table.StaticTableJiraIssueRender;

import java.util.Map;

public class JiraIssueRenderFactory {

    private StaticSingleJiraIssueRender staticSingleJiraIssueRender;

    private DynamicSingleJiraIssueRender dynamicSingleJiraIssueRender;

    private DynamicTableJiraIssueRender dynamicTableJiraIssueRender;

    private StaticTableJiraIssueRender staticTableJiraIssueRender;

    private CountJiraIssueRender countJiraIssueRender;

    private JiraIssueRenderFactory(StaticSingleJiraIssueRender singleJiraIssueRender, StaticTableJiraIssueRender staticTableJiraIssueRender, CountJiraIssueRender countJiraIssueRender) {
        this.staticSingleJiraIssueRender = singleJiraIssueRender;
        this.countJiraIssueRender = countJiraIssueRender;
        this.staticTableJiraIssueRender = staticTableJiraIssueRender;
    }

    public JiraIssueRender getJiraIssueRender(JiraRequestData jiraRequestData, Map<String, String> parameters)
    {
        JiraIssuesMacro.JiraIssuesType issuesType = JiraUtil.getJiraIssuesType(parameters, jiraRequestData.getRequestType(), jiraRequestData.getRequestData());

        switch (issuesType)
        {
            case SINGLE: return jiraRequestData.isStaticMode() ? staticSingleJiraIssueRender : dynamicSingleJiraIssueRender;

            case TABLE: return jiraRequestData.isStaticMode() ? staticTableJiraIssueRender : dynamicTableJiraIssueRender;

            case COUNT: return countJiraIssueRender;
        }

        return null;
    }

    public StaticSingleJiraIssueRender getSingleJiraIssueRender()
    {
        return staticSingleJiraIssueRender;
    }
}
