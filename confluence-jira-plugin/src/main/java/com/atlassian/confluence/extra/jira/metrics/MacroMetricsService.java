package com.atlassian.confluence.extra.jira.metrics;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.fugue.Option;

import com.google.common.base.Supplier;

public class MacroMetricsService
{
    private final EventPublisher eventPublisher;

    private static final String CONTEXT_KEY = JiraIssuesMacroMetricsEvent.Builder.class.getName();

    public MacroMetricsService(final EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
    }

    @Nonnull
    public JiraIssuesMacroMetricsEvent.Builder getOrCreateMetricsBuilder(JiraIssuesMacro macro, final ConversionContext conversionContext, @Nullable final Map<String, String> macroParameters)
    {
        return getCurrent(conversionContext).getOrElse(new Supplier<JiraIssuesMacroMetricsEvent.Builder>()
        {
            @Override
            public JiraIssuesMacroMetricsEvent.Builder get()
            {
                final JiraIssuesMacroMetricsEvent.Builder newBuilder = JiraIssuesMacroMetricsEvent.builder(eventPublisher)
                        .outputDeviceType(conversionContext.getOutputDeviceType())
                        .outputType(conversionContext.getOutputType());

                conversionContext.setProperty(CONTEXT_KEY, Option.some(newBuilder));
                return newBuilder;
            }
        });
    }

    @SuppressWarnings ("unchecked")
    private static Option<JiraIssuesMacroMetricsEvent.Builder> getCurrent(final ConversionContext conversionContext)
    {
        return (Option<JiraIssuesMacroMetricsEvent.Builder>) conversionContext.getProperty(CONTEXT_KEY, Option.none());
    }
}
