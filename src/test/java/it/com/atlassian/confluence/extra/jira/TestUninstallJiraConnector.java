package it.com.atlassian.confluence.extra.jira;

import org.junit.Test;

import com.atlassian.confluence.extra.jira.handlers.ConfluenceUpgradeFinishedHandler;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.it.plugin.Plugin;
import com.atlassian.confluence.it.plugin.PluginHelper;
import com.atlassian.confluence.it.plugin.SimplePlugin;
import com.atlassian.confluence.it.rpc.ConfluenceRpc;
import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;

public class TestUninstallJiraConnector extends AbstractConfluencePluginWebTestCase
{

    protected PluginHelper pluginHelper;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        pluginHelper = getPluginHelper();
    }

    @Test
    public void testUninstallJiraConnector()
    {
        // Since we rely on PluginFrameworkStartedEvent to uninstall
        // jira-connector plugin, at this point of
        // execution, the plugin must be uninstalled already.
        final Plugin plugin = new SimplePlugin(ConfluenceUpgradeFinishedHandler.JIRA_CONNECTOR_KEY, null);
        final boolean jiraConnectorAvailable = pluginHelper.isPluginInstalled(plugin);
        assertTrue("jira-connector plugin is still available", !jiraConnectorAvailable);
    }

    private PluginHelper getPluginHelper()
    {
        final ConfluenceRpc rpc = ConfluenceRpc.newInstance(getConfluenceWebTester().getBaseUrl());
        final User adminUser = new User(
                getConfluenceWebTester().getAdminUserName(),
                getConfluenceWebTester().getAdminPassword(),
                null, null);
        rpc.logIn(adminUser);

        return rpc.getPluginHelper();
    }

}
