package it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart;

import com.google.common.base.Function;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import it.com.atlassian.confluence.plugins.webdriver.helper.JiraChartHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.AbstractJiraIssueMacroDialog;


public abstract class AbstractJiraChartDialog extends AbstractJiraIssueMacroDialog
{
    public static final String BORDER_CSS_CLASS_NAME = "jirachart-border";
    public static final String JIRA_NAV_URL = "/jira/secure/IssueNavigator.jspa";
    public static final String JIRA_CHART_BASE_64_PREFIX = "data:image/png;base64";

    public <R> R getChartImage(Function<WebElement, R> checker)
    {
        return JiraChartHelper.getElementOnFrame(By.cssSelector(".wiki-content img.jira-chart-macro-img"), checker, driver);
    }

    public boolean isChartImageVisible()
    {
        return getChartImage(new Function<WebElement, Boolean>()
        {
            @Override
            public Boolean apply(WebElement element)
            {
                return element.isDisplayed();
            }
        });
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
        return getChartImage(new Function<WebElement, Boolean>()
        {
            @Override
            public Boolean apply(WebElement wrapper)
            {
                WebElement link = driver.findElement(By.cssSelector(".jira-chart-macro-wrapper .info a"));
                String href = link.getAttribute("href");
                return href.contains(JIRA_NAV_URL);
            }
        });
    }

    public boolean hadChartImage()
    {
        return getChartImage(new Function<WebElement, Boolean>()
        {

            @Override
            public Boolean apply(WebElement wrapper)
            {
                String imageSrc = wrapper.getAttribute("src");
                return imageSrc.contains(JIRA_CHART_BASE_64_PREFIX);
            }
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
        return getFrameWarningMsg(new Function<WebElement, Boolean>()
        {

            @Override
            public Boolean apply(WebElement element)
            {
                return element.isDisplayed();
            }
        });
    }

    public boolean hasWarningValWidth()
    {
        return queryPageElement(".width-error").isVisible();
    }

    public boolean hadImageInDialog()
    {
        return getChartImage(new Function<WebElement, Boolean>()
        {

            @Override
            public Boolean apply(WebElement pieImage)
            {
                // Note : currently don't know why image cannot display during testing session. Show will use 'src' attribute to check
                String imageSrc = pieImage.getAttribute("src");
                return imageSrc.contains(JIRA_CHART_BASE_64_PREFIX);
            }
        });
    }

    public void clickPreviewButton()
    {
        queryPageElement("#jira-chart-search-button").click();
    }
}
