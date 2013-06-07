package com.atlassian.confluence.extra.jira.handlers;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

public class AUIVersionConditionHandler implements InitializingBean, BeanFactoryAware, Condition{

    public static final String AUI_KEY = "com.atlassian.aui";

    private static final Logger logger = Logger.getLogger(ConfluenceUpgradeFinishedHandler.class);
    private BeanFactory beanFactory;
    private PluginAccessor pluginAccessor;
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context) {
        final Plugin jiraConnectorPlugin = pluginAccessor.getPlugin(AUI_KEY);
        boolean shouldDisplay = true;
        if (jiraConnectorPlugin != null)
        {
            String version = jiraConnectorPlugin.getPluginInformation().getVersion();
            logger.debug("AUI plugin version detected:" + version);
            Version auiCurrentVersion = new Version(version);
            Version auiVersionSupportedSelect2 = new Version("5.1.1");
            if(auiCurrentVersion.compareTo(auiVersionSupportedSelect2) >= 0) {
                logger.debug("Will not load select2 bundle resource because AUI plugin version detected:" + version + " supported select2");
                shouldDisplay = false;
            }
        }
        return shouldDisplay;
    }
    @Override
    public void afterPropertiesSet() throws Exception
    {
        pluginAccessor = (PluginAccessor) beanFactory
                .getBean(beanFactory.containsBean("pluginAccessor") ? "pluginAccessor" : "pluginManager");
    }

}
