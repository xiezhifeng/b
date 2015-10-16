package it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint;

import java.util.List;

import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;

import it.com.atlassian.confluence.plugins.webdriver.pageobjects.AbstractJiraIssueMacroDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.Select2Element;

public class JiraSprintMacroDialog extends AbstractJiraIssueMacroDialog
{
    private static String CSS_SELECTOR_SPRINT_PANEL = "#jira-sprint-form";

    @ElementBy(id = "s2id_jira-sprint-board")
    protected SelectElement boardSelect;

    @ElementBy(id = "s2id_jira-sprint-sprint")
    protected SelectElement sprintSelect;

    @ElementBy(cssSelector = ".insert-jira-sprint-macro-button")
    protected PageElement insertButton;

    public JiraSprintMacroDialog()
    {
        super("jira-sprint");
    }

    @Init
    public void bind()
    {
        waitUntilVisible();
    }

    @Override
    public PageElement getPanelBodyDialog()
    {
        PageElement panelBodyDialog = find(CSS_SELECTOR_SPRINT_PANEL);
        Poller.waitUntilTrue(panelBodyDialog.timed().isVisible());
        return panelBodyDialog;
    }

    public void insert()
    {
        insertButton.click();
    }

    public void selectBoard(String boardName)
    {

        Select2Element select = pageBinder.bind(Select2Element.class, boardSelect);
        select.openDropdown();
        select.chooseOption(boardName);
    }

    public void selectSprint(String sprintName)
    {
        Select2Element select = pageBinder.bind(Select2Element.class, sprintSelect);
        select.openDropdown();
        select.chooseOption(sprintName);
    }

    public List<String> getAllBoardOptions()
    {
        Select2Element select = pageBinder.bind(Select2Element.class, boardSelect);
        select.openDropdown();
        List<String> options = select.getAllOptions();
        options.remove(0);
        select.closeDropdown();

        return options;
    }

    public List<String> getAllSprintOptions()
    {
        Select2Element select = pageBinder.bind(Select2Element.class, sprintSelect);
        select.openDropdown();
        List<String> options = select.getAllOptions();
        options.remove(0);
        select.closeDropdown();

        return options;
    }

    public String getSelectedBoard()
    {
        Select2Element select = pageBinder.bind(Select2Element.class, boardSelect);
        return select.getSelectedOption().getText();
    }

    public String getSelectedSprint()
    {
        Select2Element select = pageBinder.bind(Select2Element.class, sprintSelect);
        return select.getSelectedOption().getText();
    }

}