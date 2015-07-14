package it.com.atlassian.confluence.plugins.webdriver.jiracharts.saving;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.utils.by.ByJquery;

import it.com.atlassian.confluence.plugins.webdriver.jiracharts.AbstractJiraChartTest;
import org.junit.Assert;
import org.junit.Test;

public class CreatedVsResolvedChartDialogWithSavingTest extends AbstractJiraChartTest
{
    @Test
    public void validateCreatedVsResolvedMacroInContentPage()
    {
        openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor().clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_CHART_MACRO_NAME);

        viewPage = editPage.save();
        PageElement pageElement = viewPage.getMainContent();

        String srcImg = pageElement.find(ByJquery.cssSelector("#main-content div img")).getAttribute("src");
        Assert.assertTrue(srcImg.contains(JIRA_CHART_BASE_64_PREFIX));
    }
}
