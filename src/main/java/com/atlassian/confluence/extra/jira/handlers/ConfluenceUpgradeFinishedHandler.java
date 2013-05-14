package com.atlassian.confluence.extra.jira.handlers;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;

public class ConfluenceUpgradeFinishedHandler implements InitializingBean, BeanFactoryAware
{

    private static final String JIRA_CONNECTOR_KEY = "com.atlassian.confluence.plugins.jira.jira-connector";

    private static final Logger log = Logger.getLogger(ConfluenceUpgradeFinishedHandler.class);

    private final PluginController pluginController;
    private BeanFactory beanFactory;
    private PluginAccessor pluginAccessor;

    public ConfluenceUpgradeFinishedHandler(PluginController pluginController)
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
        final Plugin jiraConnectorPlugin = pluginAccessor.getPlugin(JIRA_CONNECTOR_KEY);
        if (jiraConnectorPlugin != null)
        {
            log.debug("JiraConnector plugin detected, about to uninstall");
            pluginController.uninstall(jiraConnectorPlugin);
            log.debug("Finish uninstalling JiraConnector plugin");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        pluginAccessor = (PluginAccessor) beanFactory
                .getBean(beanFactory.containsBean("pluginAccessor") ? "pluginAccessor" : "pluginManager");
    }

}
