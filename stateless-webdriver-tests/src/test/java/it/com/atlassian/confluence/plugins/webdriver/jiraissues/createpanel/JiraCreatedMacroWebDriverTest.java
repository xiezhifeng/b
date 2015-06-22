package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.plugins.webdriver.page.JiraCreatedMacroDialog;
import com.atlassian.confluence.test.rpc.api.permissions.GlobalPermission;
import com.atlassian.confluence.test.rpc.api.permissions.SpacePermission;
import com.atlassian.confluence.test.stateless.ConfluenceStatelessTestRunner;
import com.atlassian.confluence.test.stateless.fixtures.*;
import com.atlassian.confluence.webdriver.pageobjects.ConfluenceTestedProduct;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.inject.Inject;

import static org.junit.Assert.assertTrue;

@RunWith(ConfluenceStatelessTestRunner.class)
public class JiraCreatedMacroWebDriverTest
{

    protected JiraCreatedMacroDialog jiraCreatedMacroDialog;
    protected EditContentPage editContentPage;

    @Inject protected static ConfluenceTestedProduct product;
    //@Inject protected static ConfluenceTestedProduct product;

    @Fixture public static GroupFixture group = GroupFixture.groupFixture().globalPermission(GlobalPermission.CONFLUENCE_ADMIN).build();
    @Fixture public static UserFixture user = UserFixture.userFixture().group(group).build();
    @Fixture public static SpaceFixture space = SpaceFixture.spaceFixture()
            .permission(user, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    @Fixture private static PageFixture page = PageFixture.pageFixture()
            .space(space)
            .title("Add Bar in Roadmap")
            .author(user)
            .build();

    @BeforeClass
    public static void loginOnce() throws Exception
    {
        //login once, so that we don't repeatedly login and waste time - this test doesn't need it
        product.login(user.get(), NoOpPage.class);
    }

    @Before
<<<<<<< Updated upstream
    public void setup() throws Exception
=======
    public void start() throws Exception
>>>>>>> Stashed changes
    {
        editContentPage = product.loginAndEdit(user.get(), page.get());
    }

    @Test
    public void testComponentsVisible()
    {
        openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.selectProject("Jira integration plugin");
        assertTrue(jiraCreatedMacroDialog.getComponents().isVisible());
    }

    protected JiraCreatedMacroDialog openJiraCreatedMacroDialog(boolean isFromMenu)
    {
        if(isFromMenu)
        {
            editContentPage.getEditor().openInsertMenu();
            jiraCreatedMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
            jiraCreatedMacroDialog.open();
            jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        }
        else
        {
            WebDriver driver  = product.getTester().getDriver();
            driver.switchTo().frame("wysiwygTextarea_ifr");
            driver.findElement(By.id("tinymce")).sendKeys("{ji");
            driver.switchTo().defaultContent();
            driver.findElement(By.cssSelector(".autocomplete-macro-jira")).click();
            jiraCreatedMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
        }
        return jiraCreatedMacroDialog;
    }
}
