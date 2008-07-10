package com.atlassian.confluence.extra.jira;

/**
 * Interface for configuring the trusted application link within JIRA plugin components
 */
public interface TrustedApplicationConfig
{
    /**
     * @param enabled Enable or disable the display of trusted application link warnings in the UI
     */
    void setTrustWarningsEnabled(boolean enabled);

    /**
     * @param enabled Enable or disable the transmission of trusted tokens to remote servers
     */
    void setUseTrustTokens(boolean enabled);

    /**
     * @return Whether trusted application link UI warnings are enabled
     */
    boolean isTrustWarningsEnabled();

    /**
     * @return Whether trusted tokens are sent to remote servers
     */
    boolean isUseTrustTokens();
}
