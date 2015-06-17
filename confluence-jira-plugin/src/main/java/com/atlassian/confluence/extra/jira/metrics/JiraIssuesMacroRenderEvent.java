package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;

@EventName("confluence.macro.metrics.jiraissues")
public class JiraIssuesMacroRenderEvent
{
    private final boolean staticMode;
    private final boolean isMobile;
    private final JiraIssuesMacro.JiraIssuesType issuesType;

    public JiraIssuesMacroRenderEvent(final boolean staticMode, final boolean isMobile, final JiraIssuesMacro.JiraIssuesType issuesType)
    {
        this.staticMode = staticMode;
        this.isMobile = isMobile;
        this.issuesType = issuesType;
    }

    public boolean isStaticMode()
    {
        return staticMode;
    }

    public boolean isMobile()
    {
        return isMobile;
    }

    public JiraIssuesMacro.JiraIssuesType getIssuesType()
    {
        return issuesType;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private boolean staticMode;
        private JiraIssuesMacro.JiraIssuesType issuesType;
        private boolean isMobile;

        public JiraIssuesMacroRenderEvent build()
        {
            return new JiraIssuesMacroRenderEvent(staticMode, isMobile, issuesType);
        }

        public Timer applinkResolutionTimer()
        {
            return new Timer()
            {
                @Override
                public Timer start()
                {
                    return this;
                }

                @Override
                public void stop()
                {

                }
            };
        }

        public Timer buildTemplateModelTimer()
        {
            return new Timer()
            {
                @Override
                public Timer start()
                {
                    return this;
                }

                @Override
                public void stop()
                {

                }
            };
        }

        public Timer appLinkRequestTimer()
        {
            return new Timer()
            {
                @Override
                public Timer start()
                {
                    return this;
                }

                @Override
                public void stop()
                {

                }
            };
        }

        public Timer templateRenderTimer(final boolean staticMode, final JiraIssuesMacro.JiraIssuesType issuesType, final boolean isMobile)
        {
            this.staticMode = staticMode;
            this.issuesType = issuesType;
            this.isMobile = isMobile;

            return new Timer()
            {
                @Override
                public Timer start()
                {
                    return this;
                }

                @Override
                public void stop()
                {

                }
            };
        }
    }
}
