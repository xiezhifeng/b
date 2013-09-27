package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroDialog;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class JiraIssuesMacroWebDriverTest extends AbstractJiraWebDriverTest
{

    private JiraMacroDialog openJiraMacroDialog(boolean isFromMenu)
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        JiraMacroDialog jiraMacroDialog;
        if(isFromMenu)
        {
            editPage.openInsertMenu();
            jiraMacroDialog = product.getPageBinder().bind(JiraMacroDialog.class);
            jiraMacroDialog.open();
        }
        else
        {
            WebDriver driver  = product.getTester().getDriver();
            driver.switchTo().frame("wysiwygTextarea_ifr");
            driver.findElement(By.id("tinymce")).sendKeys("{ji");
            driver.switchTo().defaultContent();
            driver.findElement(By.cssSelector(".autocomplete-macro-jira")).click();
            jiraMacroDialog = product.getPageBinder().bind(JiraMacroDialog.class);
        }
        return jiraMacroDialog;
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        JiraMacroDialog jiraMacroDialog = openJiraMacroDialog(true);
        jiraMacroDialog.selectMenuItem("Create New Issue");
        jiraMacroDialog.selectProject("10000");
        jiraMacroDialog.selectIssueType("6");
        jiraMacroDialog.setEpicName("TEST EPIC");
        jiraMacroDialog.setSummary("SUMMARY");
        EditContentPage editContentPage = jiraMacroDialog.insertIssue();
        waitForMacroOnEditor(editContentPage, "jira");
        List<MacroPlaceholder> listMacroChart = editContentPage.getContent().macroPlaceholderFor("jira");
        Assert.assertEquals(1, listMacroChart.size());
    }

    @Test
    public void testOpenRightDialog() throws InterruptedException
    {
        JiraMacroDialog jiraMacroDialog = openJiraMacroDialog(false);
        Assert.assertEquals(jiraMacroDialog.getSelectedMenu().getText(), "Search");
    }
}
