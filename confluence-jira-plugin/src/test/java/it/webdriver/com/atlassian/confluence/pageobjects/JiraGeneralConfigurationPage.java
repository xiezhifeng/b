package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Option;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraGeneralConfigurationPage
{

    @ElementBy(id = "defaultLocale_select")
    private SelectElement defaultLanguageSelected;

    @ElementBy(id = "edit_property")
    private PageElement updateButton;


    public void selectLanguage(String language)
    {
        Poller.waitUntilTrue(defaultLanguageSelected.timed().isVisible());
        Option option = getOptionByValue(defaultLanguageSelected, language);
        defaultLanguageSelected.select(option);
    }

    public Option getOptionByValue(SelectElement selectElement, String value)
    {
        for (Option option : selectElement.getAllOptions())
        {
            if (value.equals(option.value())) return option;
        }

        return selectElement.getSelected();
    }

    public void clickUpdateButton()
    {
        updateButton.click();
    }

}
