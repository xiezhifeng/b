package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import org.json.JSONException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class JiraChartNoAppLinkTest extends AbstractJiraTest
{
    @BeforeClass
    public static void start() throws Exception
    {
        doWebSudo(client);
        ApplinkHelper.removeAllAppLink(client, getAuthQueryString());
        product.login(user.get(), NoOpPage.class);
    }

    @Before
    public void setup() throws Exception {
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.OAUTH, client, getAuthQueryString(),  getBasicQueryString());
        super.setup();
    }

    @Test
    public void testUnauthenticate() throws InvalidOperationException, JSONException, IOException
    {
        dialogPieChart = openPieChartDialog(false);
        Poller.waitUntilTrue("Authentication link should be displayed", dialogPieChart.getAuthenticationLink().timed().isVisible());
    }

}
