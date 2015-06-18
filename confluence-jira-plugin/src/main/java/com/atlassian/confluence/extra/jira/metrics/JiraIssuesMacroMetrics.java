package com.atlassian.confluence.extra.jira.metrics;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;

import org.joda.time.Duration;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class JiraIssuesMacroMetrics
{
    private boolean staticMode;
    private JiraIssuesMacro.JiraIssuesType issuesType;
    private boolean isMobile;

    private final AtomicLong appLinkResolutionAccumulator = new AtomicLong();
    private final AtomicLong buildTemplateModelAccumulator = new AtomicLong();
    private final AtomicLong appLinkRequestAccumulator = new AtomicLong();
    private final AtomicLong templateRenderAccumulator = new AtomicLong();

    public JiraIssuesMacroRenderEvent buildEvent()
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

    public Timer templateRenderTimer(final boolean staticMode, final JiraIssuesMacro.JiraIssuesType issuesType, final boolean isMobile)
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

    private static final ThreadLocal<JiraIssuesMacroMetrics> THREAD_LOCAL_METRICS = new ThreadLocal<JiraIssuesMacroMetrics>()
    {
        @Override
        protected JiraIssuesMacroMetrics initialValue()
        {
            return new JiraIssuesMacroMetrics();
        }
    };

    public static JiraIssuesMacroMetrics getThreadLocal()
    {
        return THREAD_LOCAL_METRICS.get();
    }

    public static void resetThreadLocal()
    {
        THREAD_LOCAL_METRICS.remove();
    }

    public static JiraIssuesMacroMetrics resetAndGetNewThreadLocal()
    {
        resetThreadLocal();
        return getThreadLocal();
    }

    public static Supplier<JiraIssuesMacroMetrics> threadLocalMetricsSupplier()
    {
        return new Supplier<JiraIssuesMacroMetrics>()
        {
            @Override
            public JiraIssuesMacroMetrics get()
            {
                return getThreadLocal();
            }
        };
    }
}
