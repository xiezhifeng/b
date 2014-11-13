package it.webdriver.com.atlassian.confluence.pageobjects;

import org.openqa.selenium.By;

import com.atlassian.confluence.pageobjects.component.PageComponent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPropertyPanel;

public class JiraMacroPropertyPanel extends MacroPropertyPanel implements PageComponent
{

    public void editMacro()
    {
        propertyPanelElement.find(By.className("macro-placeholder-property-panel-edit-button")).click();
    }
}
