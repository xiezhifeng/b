package it.com.atlassian.confluence.plugins.jira.selenium;


/**
 * Abtract test case supported for German language
 */
public abstract class AbstractJiraPanelTestCaseInGerman extends AbstractJiraPanelTestCase
{

    protected void setUp() throws Exception
    {
        this.loginURL= "login.action?language=de_DE";
        super.setUp();
    }
    
}
