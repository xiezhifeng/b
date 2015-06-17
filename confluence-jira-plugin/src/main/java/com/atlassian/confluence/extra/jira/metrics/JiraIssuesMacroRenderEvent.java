package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;
import org.joda.time.Duration;

@EventName("confluence.macro.metrics.jiraissues")
public class JiraIssuesMacroRenderEvent
{
    private final boolean staticMode;
    private final boolean isMobile;
    private final JiraIssuesType issuesType;
    private final Duration appLinkResolutionAccumulator;
    private final Duration buildTemplateModelAccumulator;
    private final Duration appLinkRequestAccumulator;
    private final Duration templateRenderAccumulator;

    public JiraIssuesMacroRenderEvent(
            final boolean staticMode, final boolean isMobile, final JiraIssuesType issuesType,
            final Duration appLinkResolutionAccumulator, final Duration buildTemplateModelAccumulator,
            final Duration appLinkRequestAccumulator, final Duration templateRenderAccumulator)
    {
        this.staticMode = staticMode;
        this.isMobile = isMobile;
        this.issuesType = issuesType;
        this.appLinkResolutionAccumulator = appLinkResolutionAccumulator;
        this.buildTemplateModelAccumulator = buildTemplateModelAccumulator;
        this.appLinkRequestAccumulator = appLinkRequestAccumulator;
        this.templateRenderAccumulator = templateRenderAccumulator;
    }

    public long getAppLinkResolutionAccumulatorMillis()
    {
        return appLinkResolutionAccumulator.getMillis();
    }

    public long getBuildTemplateModelAccumulatorMillis()
    {
        return buildTemplateModelAccumulator.getMillis();
    }

    public long getAppLinkRequestAccumulatorMillis()
    {
        return appLinkRequestAccumulator.getMillis();
    }

    public long getTemplateRenderAccumulatorMillis()
    {
        return templateRenderAccumulator.getMillis();
    }

    public boolean isStaticMode()
    {
        return staticMode;
    }

    public boolean isMobile()
    {
        return isMobile;
    }

    public JiraIssuesType getIssuesType()
    {
        return issuesType;
    }

    public static JiraIssuesMacroMetrics builder()
    {
        return new JiraIssuesMacroMetrics();
    }

}
