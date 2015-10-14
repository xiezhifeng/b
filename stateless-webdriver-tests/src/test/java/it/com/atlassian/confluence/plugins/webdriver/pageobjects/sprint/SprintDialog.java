package it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.Select2Element;

import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static org.hamcrest.Matchers.containsString;

public class SprintDialog extends Dialog
{
    @ElementBy(id = "s2id_jira-sprint-board")
    protected SelectElement boardSelect;

    @ElementBy(id = "s2id_jira-sprint-sprint")
    protected SelectElement sprintSelect;

    @ElementBy(cssSelector = ".insert-jira-sprint-macro-button")
    protected PageElement insertButton;

    public SprintDialog()
    {
        super("jira-sprint");
    }

    @Init
    public void bind()
    {
        waitUntilVisible();
    }

    public void selectBoard(String boardName)
    {
        select(boardSelect, boardName);
    }

    public void selectSprint(String sprintName)
    {
        select(sprintSelect, sprintName);
    }

    public List<String> getAllBoardOptions()
    {
        return getAllOptions(boardSelect);
    }

    public List<String> getAllSprintOptions()
    {
        return getAllOptions(sprintSelect);
    }

    public String getSeletedBoard()
    {
        return getSelected(boardSelect);
    }

    public String getSelectedSprint()
    {
        return getSelected(sprintSelect);
    }

    public void insert()
    {
        insertButton.click();
    }

    private List<String> getAllOptions(SelectElement selectElement)
    {
        Select2Element selector = getSelect2Element(selectElement);
        selector.openDropdown();
        List<String> options = selector.getAllOptions();
        selector.closeDropdown();

        return options;
    }

    private void select(SelectElement selectElement, String name)
    {
        Select2Element selector = getSelect2Element(selectElement);

        selector.openDropdown();
        selector.chooseOption(name);

        waitUntil(selector.getSelectedOption().withTimeout(TimeoutType.AJAX_ACTION).timed().getText(), containsString(name));
    }

    private String getSelected(SelectElement selectElement)
    {
        Select2Element selector = getSelect2Element(selectElement);
        return selector.getSelectedOption().getText();
    }

    private Select2Element getSelect2Element(PageElement selectElement)
    {
        return pageBinder.bind(Select2Element.class, selectElement);
    }
}