package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;
import com.google.common.base.Stopwatch;
import org.joda.time.Duration;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private boolean staticMode;
        private JiraIssuesType issuesType;
        private boolean isMobile;

        private final AtomicLong appLinkResolutionAccumulator = new AtomicLong();
        private final AtomicLong buildTemplateModelAccumulator = new AtomicLong();
        private final AtomicLong appLinkRequestAccumulator = new AtomicLong();
        private final AtomicLong templateRenderAccumulator = new AtomicLong();

        public JiraIssuesMacroRenderEvent build()
        {
            return new JiraIssuesMacroRenderEvent(
                    staticMode, isMobile, issuesType,
                    duration(appLinkResolutionAccumulator),
                    duration(buildTemplateModelAccumulator),
                    duration(appLinkRequestAccumulator),
                    duration(templateRenderAccumulator)
            );
        }

        private static Duration duration(Number accumulator)
        {
            return Duration.millis(accumulator.longValue());
        }

        public Timer applinkResolutionTimer()
        {
            return timer(appLinkResolutionAccumulator);
        }

        public Timer buildTemplateModelTimer()
        {
            return timer(buildTemplateModelAccumulator);
        }

        public Timer appLinkRequestTimer()
        {
            return timer(appLinkRequestAccumulator);
        }

        public Timer templateRenderTimer(final boolean staticMode, final JiraIssuesType issuesType, final boolean isMobile)
        {
            this.staticMode = staticMode;
            this.issuesType = issuesType;
            this.isMobile = isMobile;

            return timer(templateRenderAccumulator);
        }

        private static Timer timer(final AtomicLong milliSecondsAccumulator)
        {
            final Stopwatch stopwatch = new Stopwatch();
            return new Timer()
            {
                @Override
                public Timer start()
                {
                    stopwatch.start();
                    return this;
                }

                @Override
                public void stop()
                {
                    milliSecondsAccumulator.addAndGet(stopwatch.elapsedTime(MILLISECONDS));
                }
            };
        }
    }
}
