package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import java.io.IOException;

import com.atlassian.confluence.plugins.helper.ApplinkHelper;
import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;

import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Be in doubt about flaky possibility")
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
        //ApplinkHelper.removeAllAppLink(client, authArgs);
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.OAUTH, client, authArgs,  getBasicQueryString());

        // We need to refresh the editor so it can pick up the new applink configuration. We need to do
        // this now since the setUp() method already places us in the editor context
        editPage.save().edit();

        dialogPieChart = openPieChartDialog(false);

        Assert.assertTrue("Authentication link should be displayed", dialogPieChart.getAuthenticationLink().isVisible());
    }

}
