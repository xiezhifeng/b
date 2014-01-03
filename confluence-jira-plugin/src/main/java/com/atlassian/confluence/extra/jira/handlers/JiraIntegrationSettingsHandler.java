package com.atlassian.confluence.extra.jira.handlers;

import org.springframework.beans.factory.InitializingBean;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class JiraIntegrationSettingsHandler implements InitializingBean
{

    public static final String AUTH_BASIC_PARAM = "com.atlassian.integration.jira.jira-integration-plugin:auth.basic.allow";

    private PluginSettingsFactory pluginSettingsFactory;

    public void setPluginSettingsFactory(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    public void afterPropertiesSet() throws Exception
    {
        setupBasicAuthenticationSetting();
    }

    private void setupBasicAuthenticationSetting()
    {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        pluginSettings.put(AUTH_BASIC_PARAM, Boolean.TRUE.toString());
    }

}
