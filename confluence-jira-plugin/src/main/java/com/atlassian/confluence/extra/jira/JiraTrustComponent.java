package com.atlassian.confluence.extra.jira;

import com.atlassian.plugin.StateAware;

/**
 * A class used to track the enable/disabled status of the trusted application link within the jira issue plugin.
 */
public class JiraTrustComponent implements StateAware
{
    private TrustedApplicationConfig trustedApplicationConfig;

    public void setTrustedApplicationConfig(TrustedApplicationConfig trustedApplicationConfig)
    {
        this.trustedApplicationConfig = trustedApplicationConfig;
    }

    public synchronized void enabled()
    {
        if (null != trustedApplicationConfig)
            trustedApplicationConfig.setUseTrustTokens(true);
    }

    public synchronized void disabled()
    {
        if (null != trustedApplicationConfig)
            trustedApplicationConfig.setUseTrustTokens(false);
    }
}
