package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart;

import it.com.atlassian.confluence.plugins.webdriver.helper.JiraChartHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.AbstractJiraIssueMacroDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraAuthenticationPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.function.Function;

public abstract class AbstractJiraChartDialog extends AbstractJiraIssueMacroDialog
{

    public static final String OAUTH_URL = "/jira/plugins/servlet/oauth/authorize";
    public static final String BORDER_CSS_CLASS_NAME = "jirachart-border";
    public static final String JIRA_NAV_URL = "/jira/secure/IssueNavigator.jspa";
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    @ElementBy(cssSelector = "#open-jira-issue-dialog")
    protected PageElement jiraIssuesMacroAnchor;

    @ElementBy(cssSelector = "#jira-chart .dialog-page-menu")
    protected PageElement dialogPageMenu;


    public AbstractJiraChartDialog()
    {
        super("jira-chart");
    }

    public <R> R getChartImage(Function<WebElement, R> checker)
    {
        return JiraChartHelper.getElementOnFrame(By.cssSelector(".wiki-content img.jira-chart-macro-img"), checker, driver);
    }

    public boolean isChartImageVisible()
    {
        return getChartImage(element -> element.isDisplayed());
    }

    /**
     * return Waring message box inside IFrame
     *
     * @return
     */
    protected <R> R getFrameWarningMsg(Function<WebElement, R> checker)
    {
        return JiraChartHelper.getElementOnFrame(By.className("aui-message"), checker, driver);
    }

    public boolean hasInfoBelowImage()
    {
        return getChartImage(wrapper -> {
            WebElement link = driver.findElement(By.cssSelector(".jira-chart-macro-wrapper .info a"));
            String href = link.getAttribute("href");
            return href.contains(JIRA_NAV_URL);
        });
    }

    public boolean hadChartImage()
    {
        return getChartImage(wrapper -> {
            String imageSrc = wrapper.getAttribute("src");
            return imageSrc.contains(JIRA_CHART_BASE_64_PREFIX);
        });
    }

    public boolean hadBorderImageInDialog()
    {
        return getChartImage(new Function<WebElement, Boolean>()
        {

            @Override
            public Boolean apply(WebElement wrapper)
            {
                String apppliedCSSClass = wrapper.getAttribute("class");
                return apppliedCSSClass.contains(BORDER_CSS_CLASS_NAME);
            }
        });
    }

    /**
     * Check whether we have warning on IFrame or not
     *
     * @return boolean
     */
    public boolean hasWarningOnIframe()
    {
        return getFrameWarningMsg(element -> element.isDisplayed());
    }

    public boolean hasWarningValWidth()
    {
        return queryPageElement(".width-error").isVisible();
    }

    public boolean hadImageInDialog()
    {
        return getChartImage(pieImage -> {
            // Note : currently don't know why image cannot display during testing session. Show will use 'src' attribute to check
            String imageSrc = pieImage.getAttribute("src");
            return imageSrc.contains(JIRA_CHART_BASE_64_PREFIX);
        });
    }

    public PageElement getAuthenticationLink()
    {
        return queryPageElement(".oauth-init");
    }

    public boolean needAuthentication()
    {
        PageElement pageElement = getPanelBodyDialog().find(By.cssSelector(".jira-chart-search .oauth-init"));
        return pageElement.isPresent() && pageElement.isVisible();
    }

    /**
     *  Do login if we have Un-trust AppLink
     */
    public void doOAuthenticate()
    {
        getAuthenticationLink().click();

        boolean isAuthenticateSuccess = false;
        //before any pop ups are open
        String parentHandle = driver.getWindowHandle();
        //after you have pop ups
        for (String popUpHandle : driver.getWindowHandles())
        {
            if(!popUpHandle.equals(parentHandle)){
                driver.switchTo().window(popUpHandle);
                // finding oauthentication page. Note we must login with JIRA first
                if(driver.getCurrentUrl().contains(OAUTH_URL)){
                    JiraAuthenticationPage authenticationPage = pageBinder.bind(JiraAuthenticationPage.class);
                    isAuthenticateSuccess = authenticationPage.doApprove();
                }
            }
        }

        Assert.assertTrue("Authenticate application link", isAuthenticateSuccess);

        // switch back to main page
        driver.switchTo().window(parentHandle);
    }

    public PageElement getJiraIssuesMacroAnchor()
    {
        Poller.waitUntilTrue(jiraIssuesMacroAnchor.timed().isVisible());
        return jiraIssuesMacroAnchor;
    }

    public JiraMacroSearchPanelDialog clickJiraIssuesMacroAnchor()
    {
        jiraIssuesMacroAnchor.click();

        JiraMacroSearchPanelDialog jiraIssueFilterDialog = this.pageBinder.bind(JiraMacroSearchPanelDialog.class);
        Poller.waitUntilTrue(jiraIssueFilterDialog.isVisibleTimed());

        return jiraIssueFilterDialog;
    }

    public String getSelectedChart()
    {
        Poller.waitUntilTrue(dialogPageMenu.timed().isVisible());

        for (PageElement chartType : dialogPageMenu.findAll(By.cssSelector(".page-menu-item")))
        {
            if (chartType.hasClass("selected"))
            {
                return chartType.getText();
            }
        }
        return "";
    }

    @Override
    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-jira-chart-macro-button", true);
        waitUntilHidden();
        return pageBinder.bind(EditContentPage.class);
    }

    public void clickPreviewButton()
    {
        queryPageElement("#jira-chart-search-button").click();
    }
}
