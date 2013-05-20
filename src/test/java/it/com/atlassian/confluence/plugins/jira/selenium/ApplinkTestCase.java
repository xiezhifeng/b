package it.com.atlassian.confluence.plugins.jira.selenium;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.confluence.it.User;
import com.atlassian.confluence.it.rpc.ConfluenceRpc;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.thoughtworks.selenium.Wait;

public class ApplinkTestCase extends AbstractJiraDialogTestCase {
    private static final String APPLINK_WS = "http://localhost:1990/confluence/rest/applinks/1.0/applicationlink";
    private static final String APPLINK_PAGE = "/confluence/admin/listapplicationlinks.action";
    private static final String CONTACTADMIN_PAGE = "/confluence/wiki/contactadministrators.action";

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        removeApplink();
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
            client.clickAndWaitForAjaxWithJquery("css=#warning-applink-dialog button.create-dialog-create-button", 3000);
            waitForWindowAppear(APPLINK_PAGE);
        }
        else
        {
            String contactAdmin = client.getText("css=#warning-applink-dialog button.button-panel-button");
            assertTrue(contactAdmin.equals("Contact admin"));
            client.click("css=#warning-applink-dialog button.button-panel-button");
            
            waitForWindowAppear(CONTACTADMIN_PAGE);
            assertTrue(checkExistWindow(CONTACTADMIN_PAGE));
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
        wait.wait("Waiting " + url + " page displayed", 3000);
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

    //remove config applink
    private void removeApplink()
    {
        WebResource webResource = null;

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("os_username", getConfluenceWebTester().getAdminUserName());
        queryParams.add("os_password", getConfluenceWebTester().getAdminPassword());
        
        List<String> ids  = new ArrayList<String>();
            
        //get list server in applink
        try
        {
            Client clientJersey = Client.create();
            webResource = clientJersey.resource(APPLINK_WS); 
            
            String result = webResource.queryParams(queryParams).accept("application/json, text/javascript, */*").get(String.class);
            final JSONObject jsonObj = new JSONObject(result);
            JSONArray jsonArray = jsonObj.getJSONArray("applicationLinks");
            for(int i = 0; i< jsonArray.length(); i++) {
                final String id = jsonArray.getJSONObject(i).getString("id");
                assertNotNull(id);
                ids.add(id);
            }
        } catch (Exception e)
        {
            assertTrue(false);
        }
        
        //delete all server config in applink
        for(String id: ids) 
        {
            String response = webResource.path(id).queryParams(queryParams).accept("application/json, text/javascript, */*").delete(String.class);
            try 
            {
                final JSONObject jsonObj = new JSONObject(response);
                int status = jsonObj.getInt("status-code");
                assertEquals(200, status);
            } catch (JSONException e) {
                assertTrue(false);
            }
        }
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
        ConfluenceRpc rpc = ConfluenceRpc.newInstance(getConfluenceWebTester().getBaseUrl());
        User adminUser = new User(
                getConfluenceWebTester().getAdminUserName(),
                getConfluenceWebTester().getAdminPassword(),
                null,
                null);
        rpc.logIn(adminUser);
        User user = new User("test","123456","test","test@atlassian.test");
        assertTrue(rpc.createUser(user));
        return user;
    }

    private void loginConfluence(String user, String password)
    {
        client.open("login.action");
        client.waitForPageToLoad();
        client.type("//input[@name = 'os_username']", user);
        client.type("//input[@name = 'os_password']", password);
        client.click("//input[@name = 'login']");
        client.waitForPageToLoad();
    }
}
