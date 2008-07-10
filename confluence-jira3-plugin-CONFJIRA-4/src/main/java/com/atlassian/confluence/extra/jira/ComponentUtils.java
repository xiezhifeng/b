package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.plugin.descriptor.MacroModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Utilties for JIRA issues plugin components
 */
class ComponentUtils
{
    private ComponentUtils()
    {

    }

    /**
     * @param pluginAccessor Plugin accessor to search
     * @return List of all macros that implement {@link TrustedApplicationConfig}
     */
    public static List getJiraTrustMacros(PluginAccessor pluginAccessor)
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
            if (module instanceof TrustedApplicationConfig)
                httpModules.add(module);
        }

        return httpModules;
    }

}
