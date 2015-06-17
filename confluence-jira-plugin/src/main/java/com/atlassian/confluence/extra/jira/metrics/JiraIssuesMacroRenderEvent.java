package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.analytics.api.annotations.EventName;

@EventName("confluence.macro.metrics.jiraissues")
public class JiraIssuesMacroRenderEvent
{
    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        Builder()
        {

        }

        public JiraIssuesMacroRenderEvent build()
        {
            return new JiraIssuesMacroRenderEvent();
        }

        public void applinkResolutionFinish()
        {

        }

        public void applinkResolutionStart()
        {

        }


        public void getJqlFromFilterStart()
        {

        }

        public void getJqlFromFilterFinish()
        {

        }

        public void buildTemplateModelFinish()
        {

        }

        public void buildTemplateModelStart()
        {

        }

        public void getColumnsInfoFromJiraFinish()
        {

        }

        public void getColumnsInfoFromJiraStart()
        {

        }

        public void appLinkRequestStart()
        {

        }

        public void appLinkRequestFinish()
        {

        }
    }
}
