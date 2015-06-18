package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;

import org.joda.time.Duration;

@EventName("confluence.macro.metrics.jiraissues")
public class JiraIssuesMacroRenderEvent
{
    private final String templateType;
    private final String deviceType;
    private final String issuesType;
    private final Duration appLinkResolutionAccumulator;
    private final Duration buildTemplateModelAccumulator;
    private final Duration appLinkRequestAccumulator;
    private final int appLinkRequestCount;
    private final Duration templateRenderAccumulator;

    public JiraIssuesMacroRenderEvent(
            final String templateType, final String deviceType, final String issuesType,
            final Duration appLinkResolutionAccumulator, final Duration buildTemplateModelAccumulator,
            final Duration appLinkRequestAccumulator, final int appLinkRequestCount, final Duration templateRenderAccumulator)
    {
        this.templateType = templateType;
        this.deviceType = deviceType;
        this.issuesType = issuesType;
        this.appLinkResolutionAccumulator = appLinkResolutionAccumulator;
        this.buildTemplateModelAccumulator = buildTemplateModelAccumulator;
        this.appLinkRequestAccumulator = appLinkRequestAccumulator;
        this.appLinkRequestCount = appLinkRequestCount;
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

    public int getAppLinkRequestCount()
    {
        return appLinkRequestCount;
    }

    public long getTemplateRenderAccumulatorMillis()
    {
        return templateRenderAccumulator.getMillis();
    }

    public String getTemplateType()
    {
        return templateType;
    }

    public String getDeviceType()
    {
        return deviceType;
    }

    public String getIssuesType()
    {
        return issuesType;
    }
}