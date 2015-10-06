package com.atlassian.confluence.extra.jira.metrics;

import javax.annotation.Nonnull;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.event.api.EventPublisher;

@EventName ("confluence.macro.metrics.jiraissues")
public class JiraIssuesMacroMetricsEvent
{
    private JiraIssuesMacroMetricsEvent(final Builder builder)
    {

    }

    public enum ExecutionType
    {

    }

    @Nonnull
    static Builder builder(final EventPublisher eventPublisher)
    {
        return new Builder(eventPublisher);
    }

    public static class Builder implements EventBuilder
    {
        private final EventPublisher eventPublisher;

        public Builder(final EventPublisher eventPublisher)
        {
            this.eventPublisher = eventPublisher;
        }

        @Override
        public void publish()
        {
            eventPublisher.publish(new JiraIssuesMacroMetricsEvent(this));
        }

        public void singleIssue()
        {

        }

        public Builder outputDeviceType(final String outputDeviceType)
        {
            return this;
        }

        public Builder outputType(final String outputType)
        {
            return this;
        }

        public void alreadyBatchProcessed()
        {
        }

        public MetricsTimer extractSingleIssueMacrosFromContent()
        {
            return null;

        }

        public void singleIssueMacrosCount(final int size)
        {

        }

        public void appLinkResolved(final boolean b)
        {

        }

        public MetricsTimer resolveAppLink()
        {
            return null;
        }

        public MetricsTimer generateSingleIssuePlaceholders(final int size)
        {
            return null;
        }

        public MetricsTimer fetchSingleIssueResults(final int size)
        {
            return null;
        }

        public MetricsTimer preFetchSingleIssuesBatch()
        {
            return null;
        }

        public Builder jiraRequestType(final JiraIssuesMacro.Type requestType)
        {
            return this;
        }

        public Builder jiraIssuesType(final JiraIssuesMacro.JiraIssuesType issuesType)
        {
            return this;
        }

        public void maximumIssues(final int maximumIssues)
        {

        }

        public MetricsTimer fetchColumnInfoFromJira()
        {
            return null;
        }
    }
}
