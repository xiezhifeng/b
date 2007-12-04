package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.renderer.radeox.macros.include.AbstractHttpRetrievalMacro;

/**
 *
 */
public abstract class JiraMacroHttpIntegrationSupport extends AbstractHttpRetrievalMacro implements JiraHttpIntegration
{
    private volatile boolean trustWarningsEnabled;

    public void setTrustedWarningsEnabled(boolean enable)
    {
        trustWarningsEnabled = enable;
    }
    
    public boolean isTrustWarningsEnabled()
    {
        return trustWarningsEnabled;
    }

    protected boolean isUserNamePasswordProvided(String url)
    {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.indexOf("os_username") != -1 && lowerUrl.indexOf("os_password") != -1;
    }
}
