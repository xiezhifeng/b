package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import com.atlassian.pageobjects.elements.PageElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class JiraChartWithSavingTest extends AbstractJiraChartTest
{
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    /**
     * validate jira image in content page
     */
    @Test
    public void validateMacroInContentPage()
    {
        openPieChartAndSearch().clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);

        viewPage = editPage.save();
        PageElement pageElement = viewPage.getMainContent();

        String srcImg = pageElement.find(By.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JIRA_CHART_BASE_64_PREFIX));
    }

 }
