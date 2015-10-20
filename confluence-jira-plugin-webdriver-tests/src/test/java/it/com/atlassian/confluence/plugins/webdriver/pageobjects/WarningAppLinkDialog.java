package it.com.atlassian.confluence.plugins.webdriver.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class WarningAppLinkDialog extends Dialog
{

    @ElementBy (cssSelector = "#warning-applink-dialog .dialog-title")
    private PageElement dialogTitle;

    @ElementBy (cssSelector = "#warning-applink-dialog a.button-panel-link")
    private PageElement cancelButton;

    @ElementBy (cssSelector = "#warning-applink-dialog .dialog-button-panel button")
    private PageElement dialogButton;

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

    public String getDialogButtonText()
    {
        Poller.waitUntilTrue(dialogButton.timed().isVisible());
        return dialogButton.getText();
    }

    @Override
    public void clickCancel()
    {
        cancelButton.click();
    }

}
