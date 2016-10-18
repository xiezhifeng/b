package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class JiraChartNoAppLinkTest extends AbstractJiraIssueMacroTest {

    @BeforeClass
    public static void start() throws Exception {
        String authArgs = getAuthQueryString();
        doWebSudo(client);
        ApplinkHelper.removeAllAppLink(client, authArgs);
        product.login(user.get(), NoOpPage.class);
    }

    @Test
    public void testUnauthenticate() throws InvalidOperationException, JSONException, IOException {
        String authArgs = getAuthQueryString();
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.OAUTH, client, authArgs,  getBasicQueryString());

        // We need to refresh the editor so it can pick up the new applink configuration. We need to do
        // this now since the setUp() method already places us in the editor context
        editPage.save().edit();
        dialogPieChart = openPieChartDialog(false);
        Poller.waitUntilTrue(
                "Authentication link should be displayed",
                dialogPieChart.getAuthenticationLink().timed().isVisible()
        );
    }

}
