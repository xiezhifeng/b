package it.com.atlassian.confluence.plugins.webdriver.jiracharts.pageview;

import com.atlassian.pageobjects.elements.PageElement;

import it.com.atlassian.confluence.plugins.webdriver.jiracharts.AbstractJiraChartTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

public class JiraChart extends AbstractJiraChartTest
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