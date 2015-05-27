package it.webdriver.com.atlassian.confluence.pageobjects.jirachart;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public abstract class JiraChartDialog extends Dialog
{

    @ElementBy(className = "insert-jira-chart-macro-button")
    protected PageElement insertMacroBtn;

    protected JiraChartDialog(String id)
    {
        super(id);
    }

    public EditContentPage clickInsertDialog()
    {
        // wait until insert button is available
        insertMacroBtn.timed().isEnabled();
        clickButton("insert-jira-chart-macro-button", true);

        return pageBinder.bind(EditContentPage.class);
    }

}
