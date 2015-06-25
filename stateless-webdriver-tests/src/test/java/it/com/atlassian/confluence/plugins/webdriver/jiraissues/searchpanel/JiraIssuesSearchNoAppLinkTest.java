package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.plugins.helper.ApplinkHelper;
import com.atlassian.confluence.plugins.pageobjects.WarningAppLinkDialog;
import com.atlassian.confluence.test.rpc.api.permissions.GlobalPermission;
import com.atlassian.confluence.test.rpc.api.permissions.SpacePermission;
import com.atlassian.confluence.test.stateless.fixtures.Fixture;
import com.atlassian.confluence.test.stateless.fixtures.GroupFixture;
import com.atlassian.confluence.test.stateless.fixtures.SpaceFixture;
import com.atlassian.confluence.test.stateless.fixtures.UserFixture;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JiraIssuesSearchNoAppLinkTest extends AbstractJiraIssuesSearchPanelWebDriverTest {

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

    @BeforeClass
    public static void start() throws Exception
    {
        String authArgs = getAuthQueryString();
        doWebSudo(client);

        if (!TestProperties.isOnDemandMode()) {
            ApplinkHelper.removeAllAppLink(client, authArgs);

        }
        product.login(user.get(), NoOpPage.class);
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

        product.loginAndEdit(userNonAdmin.get(), spaceNonAdmin.get().getHomepageRef().get());
        validateWarningDialog("Contact admin");
    }

    private void validateWarningDialog(String buttonText)
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);
        macroBrowserDialog.searchForFirst("embed jira issues").select();

        WarningAppLinkDialog warningAppLinkDialog = product.getPageBinder().bind(WarningAppLinkDialog.class);
        Assert.assertEquals("Connect Confluence To JIRA", warningAppLinkDialog.getDialogTitle());
        Assert.assertEquals(buttonText, warningAppLinkDialog.getDialogButtonText());

        warningAppLinkDialog.clickCancel();
        warningAppLinkDialog.waitUntilHidden();
    }
}
