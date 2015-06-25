package com.atlassian.confluence.plugins.pageobjects.jirachart;

import com.atlassian.confluence.plugins.pageobjects.AbstractJiraIssueMacroDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;

public abstract class AbstractJiraChartDialog extends AbstractJiraIssueMacroDialog
{

    public AbstractJiraChartDialog()
    {
        super("jira-chart");
    }

    @Override
    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-jira-chart-macro-button", true);
        return pageBinder.bind(EditContentPage.class);
    }
}
