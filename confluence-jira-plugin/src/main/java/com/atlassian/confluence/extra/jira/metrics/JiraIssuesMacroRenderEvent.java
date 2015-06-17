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


        public void buildTemplateModelFinish()
        {

        }

        public void buildTemplateModelStart()
        {

        }

        public Timer appLinkRequestTimer()
        {
            return new Timer()
            {
                @Override
                public void start()
                {

                }

                @Override
                public void stop()
                {

                }
            };
        }
    }
}
