package it.com.atlassian.confluence.plugins.webdriver.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import org.junit.Assert;

public class JiraAuthenticationPage extends ConfluenceAbstractPageComponent
{

    @ElementBy(id = "approve")
    private PageElement clickToAuthenticate;

    @ElementBy(id = "login-form-username")
    private PageElement inputUserName;

    @ElementBy(id = "login-form-password")
    private PageElement inputPass;

    @ElementBy(id = "login-form-submit")
    private PageElement loginButton;

    public boolean doApprove()
    {
        if (clickToAuthenticate.isPresent())
        {
            clickToAuthenticate.click();
        }
        else if (inputUserName.isPresent() && inputPass.isPresent())
        {
            inputUserName.type("admin");
            inputPass.type("admin");
            loginButton.click();

            Poller.waitUntilTrue(clickToAuthenticate.timed().isPresent());
            Poller.waitUntilTrue(clickToAuthenticate.timed().isVisible());
            clickToAuthenticate.click();
        }
        else
        {
            Assert.fail("Cannot find Authenticate button on Jira Authentication page");
        }

        return true;
    }
}
