package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class WarningAppLinkDialog extends Dialog
{

    @ElementBy (cssSelector = "#warning-applink-dialog .dialog-title")
    private PageElement dialogTitle;

    @ElementBy (cssSelector = "#warning-applink-dialog .button-panel-link")
    private PageElement cancelButton;

    public WarningAppLinkDialog()
    {
        super("warning-applink-dialog");
    }

    @Init
    public void bind()
    {
        waitUntilVisible();
    }

    public String getDialogTitle()
    {
        return dialogTitle.getText();
    }

    @Override
    public void clickCancel()
    {
        cancelButton.click();
    }

}
