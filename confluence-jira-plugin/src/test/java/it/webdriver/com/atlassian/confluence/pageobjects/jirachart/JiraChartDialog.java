package it.webdriver.com.atlassian.confluence.pageobjects.jirachart;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

import java.util.List;

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

    private PageElement findDialogBody()
    {
        List<PageElement> dialogBody = getDialog().findAll(By.cssSelector(".dialog-panel-body"));
        for (PageElement pageElement: dialogBody)
        {
            if(pageElement.isVisible())
            {
                return pageElement;
            }
        }
        return null;
    }

    protected PageElement openAndFindDisplayOptionElement(String cssSelector)
    {
        return  openAndFindDisplayOptionElement(cssSelector, PageElement.class);
    }

    protected <T extends PageElement> T openAndFindDisplayOptionElement(String cssSelector, Class<T> elementClass)
    {
        if (getDialog().find(By.className("jirachart-display-opts-close")).isPresent())
        {
            return getDialog().find(By.cssSelector(cssSelector), elementClass);
        }

        findDialogBody().find(By.className("jirachart-display-opts-open")).click();
        T element = getDialog().find(By.cssSelector(cssSelector), elementClass);
        Poller.waitUntilTrue(element.timed().isVisible());
        return element;
    }

}
