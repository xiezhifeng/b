package com.atlassian.confluence.plugins.jira.render;

import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.plugins.jira.render.count.CountJiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.single.DynamicSingleJiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.single.StaticSingleJiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.table.DynamicTableJiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.table.StaticTableJiraIssueRender;

public class JiraIssueRenderFactory
{

    private final StaticSingleJiraIssueRender staticSingleJiraIssueRender;

    private final DynamicSingleJiraIssueRender dynamicSingleJiraIssueRender;

    private final DynamicTableJiraIssueRender dynamicTableJiraIssueRender;

    private final StaticTableJiraIssueRender staticTableJiraIssueRender;

    private final CountJiraIssueRender countJiraIssueRender;

    private JiraIssueRenderFactory(StaticSingleJiraIssueRender singleJiraIssueRender, StaticTableJiraIssueRender staticTableJiraIssueRender, CountJiraIssueRender countJiraIssueRender,
                                   DynamicSingleJiraIssueRender dynamicSingleJiraIssueRender, DynamicTableJiraIssueRender dynamicTableJiraIssueRender)
    {
        this.staticSingleJiraIssueRender = singleJiraIssueRender;
        this.countJiraIssueRender = countJiraIssueRender;
        this.staticTableJiraIssueRender = staticTableJiraIssueRender;
        this.dynamicSingleJiraIssueRender = dynamicSingleJiraIssueRender;
        this.dynamicTableJiraIssueRender = dynamicTableJiraIssueRender;
    }

    public JiraIssueRender getJiraIssueRender(JiraRequestData jiraRequestData)
    {
        switch (jiraRequestData.getIssuesType())
        {
            case SINGLE: return jiraRequestData.isStaticMode() ? staticSingleJiraIssueRender : dynamicSingleJiraIssueRender;

            case COUNT: return countJiraIssueRender;

            default: return jiraRequestData.isStaticMode() ? staticTableJiraIssueRender : dynamicTableJiraIssueRender;
        }
    }

    public StaticSingleJiraIssueRender getSingleJiraIssueRender()
    {
        return staticSingleJiraIssueRender;
    }
}
