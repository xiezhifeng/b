package com.atlassian.confluence.extra.jira.metrics;

import java.util.Objects;

import javax.annotation.concurrent.NotThreadSafe;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;

import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;

import org.joda.time.Duration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.joda.time.Duration.millis;

@NotThreadSafe
public class JiraIssuesMacroMetrics
{
    private TemplateType templateType;
    private JiraIssuesType issuesType;
    private DeviceType deviceType;

    private final Accumulator appLinkResolutionAccumulator = new Accumulator();
    private final Accumulator buildTemplateModelAccumulator = new Accumulator();
    private final Accumulator appLinkRequestAccumulator = new Accumulator();
    private final Accumulator templateRenderAccumulator = new Accumulator();

    private enum TemplateType
    {
        STATIC, DYNAMIC
    }

    private enum DeviceType
    {
        MOBILE, DESKTOP
    }


    public JiraIssuesMacroRenderEvent buildEvent()
    {
        return new JiraIssuesMacroRenderEvent(
                Objects.toString(templateType), Objects.toString(deviceType), Objects.toString(issuesType),
                appLinkResolutionAccumulator.elapsedTime(),
                buildTemplateModelAccumulator.elapsedTime(),
                appLinkRequestAccumulator.elapsedTime(),
                appLinkRequestAccumulator.count(),
                templateRenderAccumulator.elapsedTime()
        );
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

    public Timer templateRenderTimer(final boolean staticTemplate, final JiraIssuesType issuesType, final boolean isMobile)
    {
        this.templateType = staticTemplate ? TemplateType.STATIC : TemplateType.DYNAMIC;
        this.issuesType = issuesType;
        this.deviceType = isMobile ? DeviceType.MOBILE : DeviceType.DESKTOP;

        return timer(templateRenderAccumulator);
    }

    private static Timer timer(final Accumulator accumulator)
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
                accumulator.accumulateFrom(stopwatch);
            }
        };
    }

    @NotThreadSafe
    private static class Accumulator
    {
        private long elapsedTimeMilliSeconds;
        private int count;

        public void accumulateFrom(Stopwatch stopwatch)
        {
            count++;
            elapsedTimeMilliSeconds += stopwatch.elapsedTime(MILLISECONDS);
        }

        public int count()
        {
            return count;
        }

        public Duration elapsedTime()
        {
            return millis(elapsedTimeMilliSeconds);
        }
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
