package com.atlassian.confluence.plugins.pageobjects.jirachart;

import com.atlassian.confluence.plugins.pageobjects.JiraIssueMacroDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;

public abstract class JiraChartDialog extends JiraIssueMacroDialog
{

    protected JiraChartDialog(String id)
    {
        super(id);
    }

    @Override
    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-jira-chart-macro-button", true);
        return pageBinder.bind(EditContentPage.class);
    }

}
