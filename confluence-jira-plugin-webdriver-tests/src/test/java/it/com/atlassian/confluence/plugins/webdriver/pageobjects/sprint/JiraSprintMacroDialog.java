package it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint;

import java.util.List;

import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

import it.com.atlassian.confluence.plugins.webdriver.pageobjects.AbstractJiraIssueMacroDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.Select2Element;
import org.openqa.selenium.By;

import static org.hamcrest.Matchers.is;

public class JiraSprintMacroDialog extends AbstractJiraIssueMacroDialog
{
    @ElementBy(id = "s2id_jira-sprint-board")
    protected SelectElement boardSelect;

    @ElementBy(id = "s2id_jira-sprint-sprint")
    protected SelectElement sprintSelect;

    @Init
    public void bind()
    {
        waitUntilVisible();
    }

    public void selectBoard(String boardName)
    {
        waitUntilSprintBoardLoaded();
        Select2Element select = pageBinder.bind(Select2Element.class, boardSelect);
        select.openDropdown();
        select.chooseOption(boardName);
    }

    public void selectSprint(String sprintName)
    {
        waitUntilSprintLoaded();
        Select2Element select = pageBinder.bind(Select2Element.class, sprintSelect);
        select.openDropdown();
        select.chooseOption(sprintName);
    }

    public List<String> getAllBoardOptions()
    {
        waitUntilSprintBoardLoaded();
        Select2Element select = pageBinder.bind(Select2Element.class, boardSelect);
        select.openDropdown();
        List<String> options = select.getAllOptions();
        options.remove(0);
        select.closeDropdown();

        return options;
    }

    public List<String> getAllSprintOptions()
    {
        waitUntilSprintLoaded();
        Select2Element select = pageBinder.bind(Select2Element.class, sprintSelect);
        select.openDropdown();
        List<String> options = select.getAllOptions();
        options.remove(0);
        select.closeDropdown();

        return options;
    }

    public String getSelectedBoard()
    {
        waitUntilSprintBoardLoaded();
        Select2Element select = pageBinder.bind(Select2Element.class, boardSelect);
        return select.getSelectedOption().getText();
    }

    public String getSelectedSprint()
    {
        waitUntilSprintLoaded();
        Select2Element select = pageBinder.bind(Select2Element.class, sprintSelect);
        return select.getSelectedOption().getText();
    }

    private void waitUntilSprintBoardLoaded()
    {
        PageElement boardOption = getCurrentTabPanel().find(By.cssSelector("select.jira-boards"));
        Poller.waitUntil("Sprint Board selection field is not visible", boardOption.withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().hasClass("loading"), is(false));
    }

    private void waitUntilSprintLoaded()
    {
        PageElement sprintOption = getCurrentTabPanel().find(By.cssSelector("select.jira-sprints"));
        Poller.waitUntil("Sprint selection field is not visible", sprintOption.withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().hasClass("loading"), is(false));
    }
}