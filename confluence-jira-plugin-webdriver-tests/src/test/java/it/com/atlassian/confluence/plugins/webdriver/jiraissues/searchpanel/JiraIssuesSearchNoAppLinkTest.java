package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.it.TestProperties;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.WarningAppLinkDialog;
import com.atlassian.confluence.test.rpc.api.permissions.GlobalPermission;
import com.atlassian.confluence.test.rpc.api.permissions.SpacePermission;
import com.atlassian.confluence.test.stateless.fixtures.Fixture;
import com.atlassian.confluence.test.stateless.fixtures.GroupFixture;
import com.atlassian.confluence.test.stateless.fixtures.SpaceFixture;
import com.atlassian.confluence.test.stateless.fixtures.UserFixture;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;

public class JiraIssuesSearchNoAppLinkTest extends AbstractJiraTest
{
    @Fixture
    public static GroupFixture groupNonAdmin = GroupFixture.groupFixture()
            .globalPermission(GlobalPermission.CAN_USE).build();

    @Fixture
    public static UserFixture userNonAdmin = UserFixture.userFixture()
            .group(groupNonAdmin)
            .build();

    @Fixture
    public static SpaceFixture spaceNonAdmin = SpaceFixture.spaceFixture()
            .permission(userNonAdmin, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    protected EditContentPage editPage;
    protected WarningAppLinkDialog warningAppLinkDialog;

    @BeforeClass
    public static void init() throws Exception
    {
        String authArgs = getAuthQueryString();
        doWebSudo(client);

        ApplinkHelper.removeAllAppLink(client, authArgs);
        product.login(user.get(), NoOpPage.class);
    }

    @Before
    public void setup() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @Test
    public void testSearchWithoutAppLinksWithAdmin()
    {
        validateWarningDialog("Set connection");
    }

    @Test
    public void testSearchWithoutAppLinksWithNonAdmin()
    {
        cancelEditPage(editPage);
        product.logOut();

        product.loginAndEdit(userNonAdmin.get(), spaceNonAdmin.get().getHomepageRef().get());
        validateWarningDialog("Contact admin");
    }

    protected void validateWarningDialog(String buttonText)
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);
        macroBrowserDialog.searchForFirst("embed jira issues").select();

        warningAppLinkDialog = pageBinder.bind(WarningAppLinkDialog.class);
        Assert.assertEquals("Connect Confluence To JIRA", warningAppLinkDialog.getDialogTitle());
        Assert.assertEquals(buttonText, warningAppLinkDialog.getDialogButtonText());

        warningAppLinkDialog.clickCancel();
        warningAppLinkDialog.waitUntilHidden();
    }
}
