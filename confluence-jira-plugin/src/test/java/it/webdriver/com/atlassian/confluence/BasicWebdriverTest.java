package it.webdriver.com.atlassian.confluence;

import org.junit.Test;

import junit.framework.Assert;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.confluence.webdriver.AbstractWebDriverTest;

public class BasicWebdriverTest extends AbstractWebDriverTest
{
    @Test
    public void basicLogin()
    {
        product.login(User.ADMIN, ViewPage.class, Page.TEST);
        Assert.assertTrue(product.gotoHomePage().getHeader().isAdmin());
    }
}
