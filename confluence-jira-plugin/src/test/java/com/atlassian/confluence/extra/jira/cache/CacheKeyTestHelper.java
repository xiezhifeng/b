package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginInformation;

import static com.atlassian.confluence.extra.jira.util.JiraUtil.JIRA_PLUGIN_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CacheKeyTestHelper
{
    public static void getPluginVersionExpectations(PluginAccessor mockedPluginAccessor, String version) {
        Plugin plugin = mock(Plugin.class);
        PluginInformation pluginInformation = mock(PluginInformation.class);
        when(mockedPluginAccessor.getPlugin(JIRA_PLUGIN_KEY)).thenReturn(plugin);
        when(plugin.getPluginInformation()).thenReturn(pluginInformation);
        when(pluginInformation.getVersion()).thenReturn(version);
    }
}
