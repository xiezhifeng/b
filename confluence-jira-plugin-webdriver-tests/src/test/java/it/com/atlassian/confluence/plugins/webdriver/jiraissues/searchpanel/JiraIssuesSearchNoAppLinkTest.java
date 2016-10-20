package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.test.rpc.api.permissions.GlobalPermission;
import com.atlassian.confluence.test.rpc.api.permissions.SpacePermission;
import com.atlassian.confluence.test.stateless.fixtures.Fixture;
import com.atlassian.confluence.test.stateless.fixtures.GroupFixture;
import com.atlassian.confluence.test.stateless.fixtures.SpaceFixture;
import com.atlassian.confluence.test.stateless.fixtures.UserFixture;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.WarningAppLinkDialog;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JiraIssuesSearchNoAppLinkTest extends AbstractJiraIssueMacroTest
{
    @Fixture
    private static GroupFixture groupNonAdmin = GroupFixture.groupFixture()
            .globalPermission(GlobalPermission.CAN_USE).build();

    @Fixture
    private static UserFixture userNonAdmin = UserFixture.userFixture()
            .group(groupNonAdmin)
            .build();

    @Fixture
    private static SpaceFixture spaceNonAdmin = SpaceFixture.spaceFixture()
            .permission(userNonAdmin, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    private WarningAppLinkDialog warningAppLinkDialog;

    @BeforeClass
    public static void start() throws Exception
    {
        webSudo();
        ApplinkHelper.removeAllAppLink(client, getAuthQueryString());
        product.login(user.get(), NoOpPage.class);
    }

    @After
    public void clear() throws Exception
    {
        closeDialog(warningAppLinkDialog);
    }

    @Test
    public void testSearchWithoutAppLinksWithAdmin()
    {
        validateWarningDialog("Set connection");
    }

    @Test
    public void testSearchWithoutAppLinksWithNonAdmin()
    {
        cancelEditPage();
        product.logOut();
        product.loginAndEdit(userNonAdmin.get(), spaceNonAdmin.get().getHomepageRef().get());
        validateWarningDialog("Contact admin");
    }

    private void validateWarningDialog(String buttonText)
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("embed jira issues").select();

        warningAppLinkDialog = pageBinder.bind(WarningAppLinkDialog.class);
        Assert.assertEquals("Connect Confluence To JIRA", warningAppLinkDialog.getDialogTitle());
        Assert.assertEquals(buttonText, warningAppLinkDialog.getDialogButtonText());

        warningAppLinkDialog.clickCancel();
        warningAppLinkDialog.waitUntilHidden();
    }
}
