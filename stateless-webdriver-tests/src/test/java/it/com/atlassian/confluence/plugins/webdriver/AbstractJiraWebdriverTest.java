package it.com.atlassian.confluence.plugins.webdriver;

public class AbstractJiraWebDriverTest
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    public static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");
    public static final String JIRA_ISSUE_MACRO_NAME = "jira";
    public static final String OLD_JIRA_ISSUE_MACRO_NAME = "jiraissues";
}
