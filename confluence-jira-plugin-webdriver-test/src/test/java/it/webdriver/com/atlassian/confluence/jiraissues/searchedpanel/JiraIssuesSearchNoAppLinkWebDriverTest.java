package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.WarningAppLinkDialog;

@Ignore
public class JiraIssuesSearchNoAppLinkWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{

    @Override
    protected void setup() throws Exception
    {
        authArgs = getAuthQueryString();
        doWebSudo(client);

        if (!TestProperties.isOnDemandMode())
        {
            ApplinkHelper.removeAllAppLink(client, authArgs);

        }
        editContentPage = product.loginAndEdit(User.ADMIN, Page.TEST);
    }

    @Test
    public void testSearchWithoutAppLinksWithAdmin()
    {
        validateWarningDialog("Set connection");
    }

    @Test
    public void testSearchWithoutAppLinksWithNonAdmin()
    {
        product.logOut();
        product.loginAndEdit(User.TEST, Page.TEST);
        validateWarningDialog("Contact admin");
    }

    private void validateWarningDialog(String buttonText)
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("embed jira issues").select();

        WarningAppLinkDialog warningAppLinkDialog =  product.getPageBinder().bind(WarningAppLinkDialog.class);
        Assert.assertEquals("Connect Confluence To JIRA", warningAppLinkDialog.getDialogTitle());
        Assert.assertEquals(buttonText, warningAppLinkDialog.getDialogButtonText());

        warningAppLinkDialog.clickCancel();
        warningAppLinkDialog.waitUntilHidden();
    }
}
