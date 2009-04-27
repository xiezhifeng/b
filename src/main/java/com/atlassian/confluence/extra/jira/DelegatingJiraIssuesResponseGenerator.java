package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class DelegatingJiraIssuesResponseGenerator implements JiraIssuesResponseGenerator
{
    private List<DelegatableJiraIssuesResponseGenerator> delegatableJiraIssuesResponseWriters;

    public DelegatingJiraIssuesResponseGenerator(List<DelegatableJiraIssuesResponseGenerator> delegatableJiraIssuesResponseWriters)
    {
        this.delegatableJiraIssuesResponseWriters = delegatableJiraIssuesResponseWriters;
    }

    private DelegatableJiraIssuesResponseGenerator findFirstWriterSuitableForResponse(JiraIssuesManager.Channel channel)
    {
        for (DelegatableJiraIssuesResponseGenerator delegatableJiraIssuesResponseWriter : delegatableJiraIssuesResponseWriters)
            if (delegatableJiraIssuesResponseWriter.handles(channel))
                return delegatableJiraIssuesResponseWriter;

        throw new IllegalStateException("Unable to find any JiraIssuesResponseGenerator that can handle " + channel);
    }

    public String generate(JiraIssuesManager.Channel channel, Collection<String> columnNames, int requestedPage, boolean showCount) throws IOException
    {
        return findFirstWriterSuitableForResponse(channel).generate(channel, columnNames, requestedPage, showCount);
    }
}
