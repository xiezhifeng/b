package it.webdriver.com.atlassian.confluence.jiracharts;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.jirachart.PieChartDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.confluence.security.InvalidOperationException;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.utils.by.ByJquery;


@Ignore
public class JiraChartWebDriverTest extends AbstractJiraWebDriverTest
{

    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    private PieChartDialog pieChartDialog = null;

    private JiraIssuesDialog jiraIssuesDialog;

    @After
    public void tearDown() throws Exception
    {
        if (pieChartDialog != null && pieChartDialog.isVisible())
        {
         // for some reason Dialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            pieChartDialog.clickCancel();
            pieChartDialog.waitUntilHidden();
        }
        super.tearDown();
    }

    private PieChartDialog openSelectJiraMacroDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("jira chart").select();
        return this.product.getPageBinder().bind(PieChartDialog.class);
    }

    @Test
    public void testUnauthenticate() throws InvalidOperationException, JSONException, IOException
    {
        ApplinkHelper.removeAllAppLink(client, authArgs);
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.OAUTH, client, authArgs);

        // We need to refresh the editor so it can pick up the new applink configuration. We need to do
        // this now since the setUp() method already places us in the editor context
        editContentPage.save().edit();

        pieChartDialog = openSelectJiraMacroDialog();

        Assert.assertTrue("Authentication link should be displayed", pieChartDialog.getAuthenticationLink().isVisible());
        ApplinkHelper.removeAllAppLink(client, authArgs);
    }
 }
