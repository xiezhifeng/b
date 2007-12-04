package com.atlassian.confluence.extra.jira;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.StateAware;
import com.atlassian.confluence.plugin.descriptor.MacroModuleDescriptor;

import java.util.*;

/**
 * A class used to track the enable/disabled status of warnings with the jira issue plugin. Will update warning state of
 * plugin macros implementing {@link JiraHttpIntegration} when enabled or disabled.
 */
public class JiraTrustWarnings implements StateAware
{
    private PluginAccessor pluginAccessor;

    public void setPluginAccessor(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public synchronized void enabled()
    {
        setHttpMacroWarningState(true);
    }

    /**
     * @return List of all macros that implement {@link JiraHttpIntegration}
     */
    private List getJiraHttpMacros()
    {
        Plugin plugin = pluginAccessor.getPlugin("confluence.extra.jira");
        Collection jiraModuleDescriptors = plugin.getModuleDescriptors();
        List httpModules = new LinkedList();

        for (Iterator moduleIterator = jiraModuleDescriptors.iterator(); moduleIterator.hasNext(); )
        {
            ModuleDescriptor moduleDescriptor = (ModuleDescriptor) moduleIterator.next();
            if (!(moduleDescriptor instanceof MacroModuleDescriptor))
                continue;
            
            Object module = moduleDescriptor.getModule();
            if (module instanceof JiraHttpIntegration)
                httpModules.add(module);
        }

        return httpModules;
    }

    private void setHttpMacroWarningState(boolean warningsEnabled)
    {
        for (Iterator httpModuleIterator = getJiraHttpMacros().iterator(); httpModuleIterator.hasNext(); )
            ((JiraHttpIntegration) httpModuleIterator.next()).setTrustedWarningsEnabled(warningsEnabled);
    }

    public synchronized void disabled()
    {
        setHttpMacroWarningState(false);
    }
}
