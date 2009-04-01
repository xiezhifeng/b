package com.atlassian.confluence.extra.jira;

import com.atlassian.plugin.PluginAccessor;

import java.util.List;
import java.util.Iterator;

/**
 * Support for globally modifying the state of macros that implement {@link com.atlassian.confluence.extra.jira.TrustedApplicationConfig}
 * within the JIRA plugin.
 */
abstract class AbstractJiraIssuesConfigComponent
{
    private PluginAccessor pluginAccessor;

    /**
     * @param pluginAccessor Plugin accessor to use for locating the macros
     */
    public void setPluginAccessor(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    /**
     * Globally enable or disable trusted application link UI warnings
     * @param warningsEnabled enable or disable warnings
     */
    protected final void setTrustWarningsEnabledState(boolean warningsEnabled)
    {
        List httpMacros = ComponentUtils.getJiraTrustMacros(pluginAccessor);
        for (Iterator httpModuleIterator = httpMacros.iterator(); httpModuleIterator.hasNext(); )
            ((TrustedApplicationConfig) httpModuleIterator.next()).setTrustWarningsEnabled(warningsEnabled);
    }

    /**
     * Globally enable or disable the use of trusted tokens
     * @param useTrustedTokens enable or disable trusted tokens
     */
    protected final void setUseTrustTokensState(boolean useTrustedTokens)
    {
        List httpMacros = ComponentUtils.getJiraTrustMacros(pluginAccessor);
        for (Iterator httpModuleIterator = httpMacros.iterator(); httpModuleIterator.hasNext(); )
            ((TrustedApplicationConfig) httpModuleIterator.next()).setUseTrustTokens(useTrustedTokens);
    }
}
