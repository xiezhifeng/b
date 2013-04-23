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
        if(isAdministrator) {
        	assertThat.attributeContainsValue("css=a#jira_open_applinks", "href", hrefLink);
        	client.clickAndWaitForAjaxWithJquery("css=a#jira_open_applinks", 3000);
        	assertThat.textNotPresentByTimeout("Connect Confluence To Jira", 1000);
        } else {
        	assertThat.attributeContainsValue("css=a#jira_open_contactadmin", "href", hrefLink);
        	client.clickAndWaitForAjaxWithJquery("css=a#jira_open_contactadmin", 3000);
        	assertThat.textNotPresentByTimeout("Connect Confluence To Jira", 1000);
        }
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
		} catch (Exception e) {
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
    
    private void openJiraDialogCheckAppLink()
    {
        assertThat.elementPresentByTimeout("jiralink", 10000);
        client.click("jiralink");
        assertThat.textPresentByTimeout("Connect Confluence To Jira", 1000);
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
