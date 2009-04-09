package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestDelegatingJiraIssuesResponseGenerator extends TestCase
{
    private List<DelegatableJiraIssuesResponseGenerator> delegatableJiraIssuesResponseGeneratorList;

    private DelegatingJiraIssuesResponseGenerator delegatingJiraIssuesResponseGenerator;

    @Mock private DelegatableJiraIssuesResponseGenerator delegatableJiraIssuesResponseGenerator;

    @Mock private JiraIssuesManager.Channel channel;

    private List<String> columnNames;

    private int requestedPage;

    private boolean showCount;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        delegatableJiraIssuesResponseGeneratorList = Arrays.asList(delegatableJiraIssuesResponseGenerator);

        delegatingJiraIssuesResponseGenerator = new DelegatingJiraIssuesResponseGenerator(delegatableJiraIssuesResponseGeneratorList);

        columnNames = new ArrayList<String>();
    }

    public void testGenerateThrowsExceptionIfNoSuitableGeneratorCanBeFound() throws IOException
    {
        try
        {
            delegatingJiraIssuesResponseGenerator.generate(
                    channel,
                    columnNames,
                    requestedPage,
                    showCount
            );

            fail("Expected exception to be raised but everything went ok?");
        }
        catch (IllegalStateException ise)
        {
            /* Woohoo */
        }
    }

    public void testGenerateWithSuitableGenerator() throws IOException
    {
        String output = "output";

        when(delegatableJiraIssuesResponseGenerator.handles(channel)).thenReturn(true);
        when(delegatableJiraIssuesResponseGenerator.generate(
                channel,
                columnNames,
                requestedPage,
                showCount
        )).thenReturn(output);

        assertEquals(
                output,
                delegatingJiraIssuesResponseGenerator.generate(
                        channel,
                        columnNames,
                        requestedPage,
                        showCount
                )
        );
    }

    private class DelegatingJiraIssuesResponseGenerator extends com.atlassian.confluence.extra.jira.DelegatingJiraIssuesResponseGenerator
    {
        private DelegatingJiraIssuesResponseGenerator(List<DelegatableJiraIssuesResponseGenerator> delegatableJiraIssuesResponseWriters)
        {
            super(delegatableJiraIssuesResponseWriters);
        }
    }
}
