package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class JiraChartDialog extends Dialog
{

    
    public JiraChartDialog()
    {
        super("macro-browser-dialog");
    }

    @ElementBy(id = "macro-jirachart")
    private PageElement clickToJiraChart;
    
    @ElementBy(className = "dialog-title")
    private PageElement titleDialog;
    
    @ElementBy(id = "jira-chart-inputsearch")
    private PageElement searchInput;
    
    @ElementBy(cssSelector = ".jira-chart-search bottom")
    private PageElement previewButton;
    
    @ElementBy(className = "jira-chart-img")
    private PageElement chartImage;
    
    public JiraChartDialog open()
    {
        clickToJiraChart.click();
        waitUntilVisible();
        return this;
    }

    public String getTitleDialog()
    {
        return titleDialog.getValue();
    }

}
