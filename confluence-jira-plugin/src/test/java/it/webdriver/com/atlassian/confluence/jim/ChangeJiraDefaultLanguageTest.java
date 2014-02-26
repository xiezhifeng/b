package it.webdriver.com.atlassian.confluence.jim;

import static org.junit.Assert.assertTrue;
import it.webdriver.com.atlassian.confluence.helper.JiraWebTester;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;

import org.junit.Test;

public class ChangeJiraDefaultLanguageTest extends AbstractJIMTest 
{

    @Override
    public void tearDown()
    {
        super.tearDown();
        JiraWebTester.getInstance().restoreJiraDefaultLanguage();
    }
	
	@Test
    public void testNewJimWhenChangeJiraDefaultLanguage()
    {
		JiraWebTester.getInstance().changeJiraDefaultLanguage();
		JiraIssuesPage issuesPage = createPageWithTableJiraIssueMacroAndJQL("project = tstt");
		assertTrue("Couldn't render issue table when JIRA changes its default language to French", 
				issuesPage.getTextContent().contains("Ouvertes")); //"Ouvertes" = "Open" in French 
    }

	@Test
	public void testVerifyExistingJimWhenJiraChangeDefaultLanguage()
	{
		JiraIssuesPage issuesPage = createPageWithTableJiraIssueMacroAndJQL("project = tstt");
		assertTrue(issuesPage.getTextContent().contains("Open"));  
		JiraWebTester.getInstance().changeJiraDefaultLanguage();
		issuesPage.clickRefreshedIcon();
		assertTrue("Couldn't render issue table when JIRA changes its default language to French", 
				issuesPage.getTextContent().contains("Ouvertes")); //"Ouvertes" = "Open" in French 
	}

}
