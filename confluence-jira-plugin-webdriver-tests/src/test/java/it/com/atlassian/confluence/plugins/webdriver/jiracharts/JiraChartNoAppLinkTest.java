package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import java.io.IOException;

import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.pageobjects.elements.query.Poller;

import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JiraChartNoAppLinkTest extends AbstractJiraChartTest
{
    @BeforeClass
    public static void start() throws Exception
    {
        String authArgs = getAuthQueryString();
        doWebSudo(client);

        ApplinkHelper.removeAllAppLink(client, authArgs);

        product.login(user.get(), NoOpPage.class);
    }

    @AfterClass
    public static void cleanup() throws Exception
    {
        String authArgs = getAuthQueryString();
        ApplinkHelper.removeAllAppLink(client, authArgs);
    }

    @Test
    public void testUnauthenticate() throws InvalidOperationException, JSONException, IOException
    {
        String authArgs = getAuthQueryString();
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.OAUTH, client, authArgs,  getBasicQueryString());

        // We need to refresh the editor so it can pick up the new applink configuration. We need to do
        // this now since the setUp() method already places us in the editor context
        editPage.cancel();
        editPage = gotoEditTestPage(user.get());

        dialogPieChart = openPieChartDialog(false);

        Poller.waitUntilTrue("Authentication link should be displayed", dialogPieChart.getAuthenticationLink().timed().isVisible());
    }

}
