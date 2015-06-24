package com.atlassian.confluence.plugins.pageobjects;

import com.atlassian.confluence.pageobjects.component.ConfluenceAbstractPageComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

import org.junit.Assert;

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
