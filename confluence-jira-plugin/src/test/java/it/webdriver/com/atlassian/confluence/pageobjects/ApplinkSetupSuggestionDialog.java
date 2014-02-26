package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.dialog.AbstractDialog;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class ApplinkSetupSuggestionDialog extends AbstractDialog
{
    public ApplinkSetupSuggestionDialog()
    {
        super("warning-applink-dialog");
    }

    @ElementBy(cssSelector = ".create-dialog-create-button")
    private PageElement setConnectionButton;

    @Init
    public void bind()
    {
        waitUntilVisible();
    }

    public boolean isSetConnectionButtonVisible()
    {
        return setConnectionButton.isVisible();
    }

    public boolean isContactAdminButtonVisible()
    {
        return this.getDialog().getText().contains("Contact admin");
    }

}
