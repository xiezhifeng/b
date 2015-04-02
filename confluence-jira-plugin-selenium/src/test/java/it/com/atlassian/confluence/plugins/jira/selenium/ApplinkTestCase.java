package it.com.atlassian.confluence.plugins.jira.selenium;


import com.atlassian.confluence.it.User;
import com.atlassian.confluence.it.rpc.ConfluenceRpc;
import com.thoughtworks.selenium.SeleniumException;
import com.thoughtworks.selenium.Wait;

public class ApplinkTestCase extends AbstractJiraDialogTestCase {
    private static final String APPLINK_PAGE = "/confluence/admin/listapplicationlinks.action";
    private static final String CONTACTADMIN_PAGE = "/confluence/wiki/contactadministrators.action";

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        doWebSudo(httpClient);
        removeApplink(httpClient, getAuthQueryString());
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    /**
     * open macro with account login is admin
     */
    public void testVerifyApplinkWithAccountAdmin() 
    {
        openWarningDialogAndVerify(true, getConfluenceWebTester().getAdminUserName(), getConfluenceWebTester().getAdminPassword(), APPLINK_PAGE);
    }

    /**
     * open macro with account login is user
     */
    public void testVerifyApplinkWithAccountNoAdmin() 
    {
        //create user test (don't have permission admin)
        User user = createUser();
        openWarningDialogAndVerify(false, user.getUsername(), user.getPassword(), CONTACTADMIN_PAGE);
    }

    //open warning dialog follow role of login's user
    private void openWarningDialogAndVerify(boolean isAdministrator, String username, String password, String hrefLink)
    {
        loginConfluence(username, password);
        client.open("pages/createpage.action?spaceKey=" + TEST_SPACE_KEY);
        client.waitForPageToLoad();
        openJiraDialogCheckAppLink();
        if(isAdministrator)
        {
            String connectAppLink = client.getText("css=#warning-applink-dialog button.create-dialog-create-button");
            assertTrue(connectAppLink.equals("Set connection"));
            // client.clickAndWaitForAjaxWithJquery("css=#warning-applink-dialog button.create-dialog-create-button",
            // 3000);
            // waitForWindowAppear(APPLINK_PAGE);
        }
        else
        {
            assertThat.elementPresentByTimeout("css=#warning-applink-dialog button.button-panel-button", 3000);
            String contactAdmin = client.getText("css=#warning-applink-dialog button.button-panel-button");
            assertTrue(contactAdmin.equals("Contact admin"));
            //client.click("css=#warning-applink-dialog button.button-panel-button");
            //waitForWindowAppear(CONTACTADMIN_PAGE);
            //assertTrue(checkExistWindow(CONTACTADMIN_PAGE));
        }
    }
    
    private void waitForWindowAppear(final String url) {
        
        Wait wait = new Wait() {
            int count = 0;
            @Override
            public boolean until() {
                //use the counter to stop because there was an error with Bamboo.
                //The code run on Bamboo forever.
                count ++;
                if(count == 10) {
                    return true;
                }
                return checkExistWindow(url);
            }
        };
        wait.wait("Waiting " + url + " page displayed", 10000);
    }
    
    private boolean checkExistWindow(String url) {
        boolean flag = false;
        String titles[] = client.getAllWindowTitles();
        for(String title : titles) 
        {
            client.selectWindow(title);
            if(client.getLocation().contains(url)) {
                return flag = true;
            }
        }
        return flag;
    }

    private void openJiraDialogCheckAppLink()
    {
        assertThat.elementPresentByTimeout("jiralink", 10000);
        client.click("jiralink");
        assertThat.textPresentByTimeout("Connect Confluence To JIRA", 1000);
    }

    //create user test with role don't permission admin 
    private User createUser()
    {
        User user = new User("test"+ System.currentTimeMillis(),"123456","test","test@atlassian.test");
        assertTrue(rpc.createUser(user));
        return user;
    }

    private void loginConfluence(String user, String password)
    {
        client.open("login.action");
        client.waitForPageToLoad();
        try
        {
            client.type("//input[@name = 'os_username']", user);
        } catch (SeleniumException e)
        {
            // already logged in, no need to have further process
            if (e.getMessage().contains("//input[@name = 'os_username'] not found"))
            {
                return;
            }
        }
        client.type("//input[@name = 'os_password']", password);
        client.click("//input[@name = 'login']");
        client.waitForPageToLoad();
    }
}
