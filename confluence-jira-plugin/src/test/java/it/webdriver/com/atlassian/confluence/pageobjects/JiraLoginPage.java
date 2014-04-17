package it.webdriver.com.atlassian.confluence.pageobjects;

import com.atlassian.confluence.it.User;
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

    public void login(User user)
    {
        userName.type(user.getUsername());
        password.type(user.getPassword());
        loginButton.click();
    }

}
