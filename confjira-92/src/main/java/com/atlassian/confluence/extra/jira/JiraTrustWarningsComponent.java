package com.atlassian.confluence.extra.jira;

import com.atlassian.plugin.StateAware;

/**
 * A component used to track the enable/disabled status of warnings within the jira issue plugin.
 */
public class JiraTrustWarningsComponent implements StateAware
{
    private TrustedApplicationConfig trustedApplicationConfig;

    public void setTrustedApplicationConfig(TrustedApplicationConfig trustedApplicationConfig)
    {
        this.trustedApplicationConfig = trustedApplicationConfig;
    }

    public synchronized void enabled()
    {
        if (null != trustedApplicationConfig)
            trustedApplicationConfig.setTrustWarningsEnabled(true);
    }

    public synchronized void disabled()
    {
        if (null != trustedApplicationConfig)
            trustedApplicationConfig.setTrustWarningsEnabled(false);
    }
}
