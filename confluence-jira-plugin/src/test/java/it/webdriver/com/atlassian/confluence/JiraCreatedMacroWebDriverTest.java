package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraCreatedMacroDialog;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class JiraCreatedMacroWebDriverTest extends AbstractJiraWebDriverTest
{

    private JiraCreatedMacroDialog openJiraCreatedMacroDialog(boolean isFromMenu)
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        JiraCreatedMacroDialog jiraMacroDialog;
        if(isFromMenu)
        {
            editPage.openInsertMenu();
            jiraMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
            jiraMacroDialog.open();
            jiraMacroDialog.selectMenuItem("Create New Issue");
        }
        else
        {
            WebDriver driver  = product.getTester().getDriver();
            driver.switchTo().frame("wysiwygTextarea_ifr");
            driver.findElement(By.id("tinymce")).sendKeys("{ji");
            driver.switchTo().defaultContent();
            driver.findElement(By.cssSelector(".autocomplete-macro-jira")).click();
            jiraMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
        }
        return jiraMacroDialog;
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        JiraCreatedMacroDialog jiraMacroDialog = openJiraCreatedMacroDialog(true);
        EditContentPage editContentPage = createJiraIssue(jiraMacroDialog, "10000", "6", "TEST EPIC", "SUMMARY");
        List<MacroPlaceholder> listMacroChart = editContentPage.getContent().macroPlaceholderFor("jira");
        Assert.assertEquals(1, listMacroChart.size());
        editContentPage.save();
    }

    @Test
    public void testOpenRightDialog() throws InterruptedException
    {
        JiraCreatedMacroDialog jiraMacroDialog = openJiraCreatedMacroDialog(false);
        Assert.assertEquals(jiraMacroDialog.getSelectedMenu().getText(), "Search");
    }
}
