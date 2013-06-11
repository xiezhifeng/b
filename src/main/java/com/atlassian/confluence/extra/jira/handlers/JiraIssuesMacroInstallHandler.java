package com.atlassian.confluence.extra.jira.handlers;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginState;

public class JiraIssuesMacroInstallHandler implements InitializingBean, BeanFactoryAware
{

    public static final String PLUGIN_KEY_JIRA_CONNECTOR = "com.atlassian.confluence.plugins.jira.jira-connector";
    public static final String PLUGIN_KEY_CONFLUENCE_PASTE = "com.atlassian.confluence.plugins.confluence-paste";

    public static final String PLUGIN_MODULE_KEY_JIRA_PASTE = "com.atlassian.confluence.plugins.confluence-paste:autoconvert-jira";

    private static final Logger log = Logger.getLogger(JiraIssuesMacroInstallHandler.class);

    private final PluginController pluginController;
    private BeanFactory beanFactory;
    private PluginAccessor pluginAccessor;

    public JiraIssuesMacroInstallHandler(PluginController pluginController)
    {
        this.pluginController = pluginController;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

    public void uninstallJiraConnectorPlugin()
    {
        final Plugin jiraConnectorPlugin = pluginAccessor.getPlugin(PLUGIN_KEY_JIRA_CONNECTOR);
        if (jiraConnectorPlugin != null)
        {
            log.debug("JiraConnector plugin detected, about to uninstall");
            pluginController.uninstall(jiraConnectorPlugin);
            log.debug("Finish uninstalling JiraConnector plugin");
        }
    }

    public void disableJiraPaste()
    {
        final Plugin jiraConfluencePastePlugin = pluginAccessor.getPlugin(PLUGIN_KEY_CONFLUENCE_PASTE);
        if (jiraConfluencePastePlugin != null && jiraConfluencePastePlugin.getPluginState() == PluginState.ENABLED) {
            pluginController.disablePluginModule(PLUGIN_MODULE_KEY_JIRA_PASTE);
            log.debug("Finish disabling JiraPaste module: " + PLUGIN_MODULE_KEY_JIRA_PASTE);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        pluginAccessor = (PluginAccessor) beanFactory
                .getBean(beanFactory.containsBean("pluginAccessor") ? "pluginAccessor" : "pluginManager");
        // This is a bit hacky way but since there's no event we can rely on to
        // do our business, especially in case of installing Jira Issues Macro
        // plugin on an old version of Confluence
        uninstallJiraConnectorPlugin();
        disableJiraPaste();
    }

}
