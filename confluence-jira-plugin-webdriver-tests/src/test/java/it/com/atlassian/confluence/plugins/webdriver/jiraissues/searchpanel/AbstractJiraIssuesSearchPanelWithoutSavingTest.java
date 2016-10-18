package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.pageobjects.elements.query.Poller;
import org.junit.Before;

/**
 * Any class inherits from this class `AbstractJiraChartWithoutSavingTest` will test for edit page only
 * and do not act saving page.
 */
public abstract class AbstractJiraIssuesSearchPanelWithoutSavingTest extends AbstractJiraIssuesSearchPanelTest
{
    /**
     * Just clear content before running each test.
     * Do not call setup method of parent class.
     * @throws Exception
     */
    @Before
    public void setup() throws Exception
    {
        Poller.waitUntilTrue("Edit page is ready", editPage.getEditor().isEditorCurrentlyActive());
        editPage.getEditor().getContent().clear().focus();
    }
 }
