package com.atlassian.confluence.extra.jira;


public class DefaultTrustedApplicationConfig implements TrustedApplicationConfig
{
    private boolean trustWarningsEnabled;

    private boolean useTrustTokens;

    public DefaultTrustedApplicationConfig()
    {
        /* By default, these are true */
        setTrustWarningsEnabled(true);
        setUseTrustTokens(true);
    }

    public boolean isTrustWarningsEnabled()
    {
        return trustWarningsEnabled;
    }

    public void setTrustWarningsEnabled(boolean trustWarningsEnabled)
    {
        this.trustWarningsEnabled = trustWarningsEnabled;
    }

    public boolean isUseTrustTokens()
    {
        return useTrustTokens;
    }

    public void setUseTrustTokens(boolean useTrustTokens)
    {
        this.useTrustTokens = useTrustTokens;
    }
}
