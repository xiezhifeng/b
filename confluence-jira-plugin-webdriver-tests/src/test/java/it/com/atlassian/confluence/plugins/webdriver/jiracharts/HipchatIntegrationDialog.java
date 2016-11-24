package it.com.atlassian.confluence.plugins.webdriver.jiracharts;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.AbstractDialog;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

/**
 * @since 7.1.5
 */
public class HipchatIntegrationDialog extends AbstractDialog {

    @ElementBy(id = "hipchat-integration-discovery-dismiss")
    private PageElement dismissDialogButton;

    public HipchatIntegrationDialog()
    {
        super("nline-dialog-hipchat-integration-dialog");
    }

    public HipchatIntegrationDialog dismiss() {
        dismissDialogButton.click();
        waitUntilHidden();
        return this;
    }
}
