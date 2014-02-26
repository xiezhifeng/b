package it.webdriver.com.atlassian.confluence.helper;

import static org.junit.Assert.fail;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;

import java.io.IOException;

import net.sourceforge.jwebunit.junit.WebTester;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;

public class JiraWebTester extends WebTester 
{

	private static class JiraWebTesterInstanceHolder 
	{
		private static final JiraWebTester instance = new JiraWebTester();
		static {
			try 
			{
				instance.setup();
				instance.loginToJira("admin","admin");
			} 
			catch (IOException e) 
			{
				fail("couldn't setup JiraWebTester : " + e.getMessage());
			}
		}
	}
	
	private JiraWebTester() {
		super();
	}
	
	public static JiraWebTester getInstance()
	{
		return JiraWebTesterInstanceHolder.instance;
	}
	
    public void changeJiraDefaultLanguage()
    {
        setJiraLanguage("fr_FR");
    }
    
    public void restoreJiraDefaultLanguage()
    {
        setJiraLanguage("-1");
    }
    
    public void setJiraLanguage(String locale) 
    {
        gotoPage("/secure/admin/EditApplicationProperties!default.jspa");
        setWorkingForm("jiraform");
        selectOptionByValue("defaultLocale", locale);
        setTextField("baseURL", AbstractJiraWebDriverTest.JIRA_BASE_URL);
        submit();
    }
	
    private void setup() throws IOException
    {
        this.setTestingEngineKey(TestingEngineRegistry.TESTING_ENGINE_HTMLUNIT);
        this.setScriptingEnabled(false);
        this.getTestContext().setBaseUrl(AbstractJiraWebDriverTest.JIRA_BASE_URL);
        this.beginAt("/");
    }

    private void loginToJira(String userName, String password)
    {
    	this.gotoPage("/login.jsp");
    	this.setWorkingForm("login-form");
    	this.setTextField("os_username", userName);
    	this.setTextField("os_password", password);
    	this.submit();
    }
}
