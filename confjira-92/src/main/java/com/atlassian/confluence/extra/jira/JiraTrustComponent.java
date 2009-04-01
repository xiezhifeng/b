package com.atlassian.confluence.extra.jira;

import com.atlassian.plugin.StateAware;

/**
 * A class used to track the enable/disabled status of the trusted application link within the jira issue plugin.
 */
public class JiraTrustComponent extends AbstractJiraIssuesConfigComponent implements StateAware
{
    public synchronized void enabled()
    {
        setUseTrustTokensState(true);
    }

    public synchronized void disabled()
    {
        setUseTrustTokensState(false);
    }
}
