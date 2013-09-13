package it.webdriver.com.atlassian.confluence.pageobjects;

import org.junit.Assert;

import com.atlassian.confluence.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class JiraAuthenticationPage extends ConfluenceAbstractPageComponent
{

    @ElementBy(id = "approve")
    private PageElement clickToAuthenticate;

    public boolean doApprove()
    {
        if (!clickToAuthenticate.isPresent())
        {
            Assert.fail("Cannot find Authenticate button on Jira Authentication page");
        }
        clickToAuthenticate.click();

        return true;
    }
}
