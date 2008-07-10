package com.atlassian.confluence.extra.jira;

import com.atlassian.plugin.StateAware;

/**
 * A component used to track the enable/disabled status of warnings within the jira issue plugin.
 */
public class JiraTrustWarningsComponent extends AbstractJiraIssuesConfigComponent implements StateAware
{
    public synchronized void enabled()
    {
        setTrustWarningsEnabledState(true);
    }

    public synchronized void disabled()
    {
        setTrustWarningsEnabledState(false);
    }
}
