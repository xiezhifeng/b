package it.com.atlassian.confluence.plugins.webdriver.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class WarningAppLinkDialog extends Dialog
{
    public static final String DIALOG_ID = "warning-applink-dialog";

    @ElementBy (cssSelector = "#" + DIALOG_ID + " .aui-dialog2-header-main")
    private PageElement dialogTitle;

    @ElementBy (cssSelector = "#" + DIALOG_ID + " .dialog-close-button")
    private PageElement cancelButton;

    @ElementBy (cssSelector = "#" + DIALOG_ID + " .dialog-submit-button")
    private PageElement dialogButton;

    public WarningAppLinkDialog()
    {
        super(DIALOG_ID);
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
