package com.atlassian.confluence.extra.jira;

/**
 * Simple implementation of a trusted application configuration. All settings default to <tt>false</tt>
 */
class JiraIssuesTrustedApplicationConfig implements TrustedApplicationConfig
{
    private volatile boolean trustWarningsEnabled = false;
    private volatile boolean useTrustTokens = false;
    
    public void setTrustWarningsEnabled(boolean enabled)
    {
        trustWarningsEnabled = enabled;
    }

    public void setUseTrustTokens(boolean enabled)
    {
        useTrustTokens = enabled;
    }

    public boolean isTrustWarningsEnabled()
    {
        return trustWarningsEnabled;
    }

    public boolean isUseTrustTokens()
    {
        return useTrustTokens;
    }
}
