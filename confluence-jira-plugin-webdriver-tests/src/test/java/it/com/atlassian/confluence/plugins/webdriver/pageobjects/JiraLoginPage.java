package it.com.atlassian.confluence.plugins.webdriver.pageobjects;

import com.atlassian.confluence.test.api.model.person.UserWithDetails;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class JiraLoginPage
{

    @ElementBy(id = "login-form-username")
    private PageElement userName;

    @ElementBy(id = "login-form-password")
    private PageElement password;

    @ElementBy(id = "login-form-submit")
    private PageElement loginButton;

    public void login(UserWithDetails user)
    {
        userName.type(user.getUsername());
        password.type(user.getPassword());
        loginButton.click();
    }
}
