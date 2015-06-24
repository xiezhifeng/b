package com.atlassian.confluence.plugins.pageobjects;

import com.atlassian.confluence.webdriver.pageobjects.component.PageComponent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPropertyPanel;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

public class JiraMacroPropertyPanel extends MacroPropertyPanel implements PageComponent
{

    public void editMacro()
    {
        propertyPanelElement.find(By.className("macro-placeholder-property-panel-edit-button")).click();
    }

    public PageElement getPropertyPanel(String cssSelector)
    {
        return propertyPanelElement.find(By.cssSelector(cssSelector));
    }
}
