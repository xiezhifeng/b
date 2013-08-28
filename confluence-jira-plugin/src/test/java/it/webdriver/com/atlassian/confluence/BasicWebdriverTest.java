package it.webdriver.com.atlassian.confluence;

import it.webdriver.com.atlassian.confluence.pageobjects.JiraChartDialog;

import org.junit.Assert;
import org.junit.Test;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.AbstractWebDriverTest;

public class BasicWebdriverTest extends AbstractWebDriverTest
{
    /*@Test
    public void basicLogin()
    {
        product.login(User.ADMIN, ViewPage.class, Page.TEST);
        Assert.assertTrue(product.gotoHomePage().getHeader().isAdmin());
    }
    */
    @Test
    public void openSelectMacroDialog()
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.openMacroBrowser();
        JiraChartDialog jiraChartDialog = product.getPageBinder().bind(JiraChartDialog.class);
        jiraChartDialog.open();
        Assert.assertTrue("Insert JIRA Chart".equals(jiraChartDialog.getTitleDialog().equals("")));
    }
}
