package it.webdriver.com.atlassian.confluence.pageobjects;

import org.openqa.selenium.By;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class JiraChartDialog extends Dialog
{
    @ElementBy(id = "macro-jirachart")
    private PageElement clickToJiraChart;
    
    @ElementBy(id = "jira-chart-inputsearch")
    private PageElement jqlSearch;
    
    @ElementBy(id = "jira-chart-border")
    private PageElement borderImage;
    
    @ElementBy(id = "jira-chart-width")
    private PageElement width;
    
    public JiraChartDialog()
    {
        super("jira-chart");
    }
    
    public JiraChartDialog open()
    {
        clickToJiraChart.click();
        return this;
    }

    public String getTitleDialog()
    {
        return driver.findElement(By.cssSelector("#jira-chart .dialog-title")).getText();
    }
    
    public JiraChartDialog inputJqlSearch(String val)
    {
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }
    
    public JiraChartDialog pasteJqlSearch(String val)
    {
        jqlSearch.type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"paste\")");
        return this;
    }
    
    public String getJqlSearch()
    {
        return jqlSearch.getValue();
    }

    public void clickPreviewButton()
    {
        driver.findElement(By.cssSelector("#jira-chart .jira-chart-search button")).click();
    }
    
    public PageElement getPageEleJQLSearch()
    {
        return jqlSearch;
    }
    
    public void clickBorderImage()
    {
        borderImage.click();
    }
    
    public void setValueWidthColumn(String val)
    {
        width.clear().type(val);
    }
    
    @SuppressWarnings("deprecation")
    public boolean hadImageInDialog()
    {
        driver.waitUntilElementIsVisible(By.cssSelector("#jira-chart .jira-chart-img .chart-img"));
        return driver.findElement(By.cssSelector("#jira-chart .jira-chart-img .chart-img")).isDisplayed();
    }
    
    @SuppressWarnings("deprecation")
    public boolean hadBorderImageInDialog()
    {
        driver.waitUntilElementIsVisible(By.cssSelector("#jira-chart .jira-chart-img .chart-img .jirachart-border"));
        return driver.findElement(By.cssSelector("#jira-chart .jira-chart-img .chart-img .jirachart-border")).isDisplayed();
    }
    
    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-jira-chart-macro-button", false);
        return pageBinder.bind(EditContentPage.class);
    }
    
    public String getLinkMoreToCome()
    {
        return driver.findElement(By.cssSelector("#jira-chart .dialog-page-menu .moreToCome a")).getAttribute("href");
    }
    
    @SuppressWarnings("deprecation")
    public boolean hasWarningValWidth()
    {
        driver.waitUntilElementIsVisible(By.cssSelector("#jira-chart .jira-chart-img .warningValWidth"));
        return driver.findElement(By.cssSelector("#jira-chart .jira-chart-img .warningValWidth")).isDisplayed();
    }

}
