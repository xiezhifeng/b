package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.pageobjects.component.PageComponent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPropertyPanel;
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
