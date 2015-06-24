package com.atlassian.confluence.plugins.pageobjects.jirachart;


import com.atlassian.confluence.plugins.helper.JiraChartHelper;
import com.atlassian.confluence.plugins.pageobjects.JiraAuthenticationPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Option;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;

import com.google.common.base.Function;
import com.ibm.icu.impl.Assert;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import it.com.atlassian.confluence.plugins.webdriver.jiracharts.JiraChartTest;

public class CreatedVsResolvedChartDialog extends AbstractJiraChartDialog
{
    public static final String OAUTH_URL = "/jira/plugins/servlet/oauth/authorize";
    public static final String BORDER_CSS_CLASS_NAME = "jirachart-border";
    public static final String JIRA_NAV_URL = "/jira/secure/IssueNavigator.jspa";
    public static final String CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED = "#jira-chart-content-createdvsresolved";

    @ElementBy(cssSelector = CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED + " #jira-chart-search-input")
    protected SelectElement jqlSearch;
    
    @ElementBy(cssSelector = CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED + " #created-vs-resolved-chart-periodName")
    protected SelectElement periodName;

    @ElementBy(cssSelector = CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED + " #created-vs-resolved-chart-daysprevious")
    protected PageElement daysPrevious;

    @ElementBy(cssSelector = CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED + " #created-vs-resolved-chart-cumulative")
    protected PageElement cumulative;

    @ElementBy(cssSelector = CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED + " #created-vs-resolved-chart-versionLabel")
    protected SelectElement versionLabel;

    @ElementBy(cssSelector = "#created-vs-resolved-chart-showunresolvedtrend")
    protected PageElement showUnResolvedTrend;

    @ElementBy(cssSelector = CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED + " .days-previous-error")
    protected PageElement daysPreviousError;
    
    @ElementBy(cssSelector = "#jira-createdvsresolved-chart-show-border")
    protected PageElement borderImage;
    
    @ElementBy(cssSelector = "#jira-createdvsresolved-chart-show-infor")
    protected PageElement showInfo;
    
    @ElementBy(cssSelector = "#oauth-init")
    protected PageElement authenticationLink;
    
    @ElementBy(cssSelector = "#jira-chart-width")
    protected PageElement width;

    public void checkDisplayCumulativeTotal()
    {
        cumulative.click();
    }

    public void checkDisplayTrendOfUnResolved()
    {
        showUnResolvedTrend.click();
    }

    public void setSelectedForPeriodName(String value)
    {
        periodName.type(value);
    }

    public Option getSelectedForPeriodName()
    {
        return periodName.getSelected();
    }

    public void setSelectedForVersionLabel(Option option)
    {
        versionLabel.select(option);
    }

    public Option getSelectedForVersionLabel()
    {
        return versionLabel.getSelected();
    }

    public String getDaysPrevious()
    {
        return daysPrevious.getValue();
    }

    public void setDaysPrevious(String value)
    {
        daysPrevious.clear().type(value);
    }

    public String getDaysPreviousError()
    {
        return daysPreviousError.getText();
    }
    
    public CreatedVsResolvedChartDialog inputJqlSearch(String val)
    {
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }
    
    public CreatedVsResolvedChartDialog pasteJqlSearch(String val)
    {
        jqlSearch.type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"paste\")");
        return this;
    }
    
    public String getJqlSearch()
    {
        return jqlSearch.getValue();
    }

    public void clickPreviewButton()
    {
        driver.findElement(By.cssSelector(CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED + " #jira-chart-search-button")).click();
    }
    
    public void clickBorderImage()
    {
        borderImage.click();
    }
    
    public void clickShowInforCheckbox(){
        showInfo.click();
    }
    
    public void setValueWidthColumn(String val)
    {
        width.clear().type(val);
    }
    
    public boolean hasInfoBelowImage(){
        return getCreatedVsResolvedImage(new Function<WebElement, Boolean>()
        {
            @Override
            public Boolean apply(WebElement imageWrapper)
            {
                WebElement link = driver.findElement(By.cssSelector("div.jira-chart-macro-wrapper div.info a"));
                String href = link.getAttribute("href");
                return href.contains(JIRA_NAV_URL);
            }
        });
    }
    
    public boolean hadImageInDialog()
    {
        return getCreatedVsResolvedImage(new Function<WebElement, Boolean>()
        {

            @Override
            public Boolean apply(WebElement createVsResolved)
            {
                // Note : currently don't know why image cannot display during testing session. Show will use 'src' attribute to check
                String imageSrc = createVsResolved.getAttribute("src");
                return imageSrc.contains(JiraChartTest.JIRA_CHART_BASE_64_PREFIX);
            }
        });
    }
    
    public boolean hadBorderImageInDialog()
    {
        return getCreatedVsResolvedImageWrapper(new Function<WebElement, Boolean>()
        {

            @Override
            public Boolean apply(WebElement createdVsResolvedImageWrapper)
            {
                String apppliedCSSClass = createdVsResolvedImageWrapper.getAttribute("class");
                return apppliedCSSClass.contains(BORDER_CSS_CLASS_NAME);
            }
        });
    }
    
    public boolean needAuthentication(){
        WebElement element = ((WebElement) driver.executeScript("return jQuery('.jira-chart-search .oauth-init')[0];"));
        return element != null;
    }
    
    /**
     *  Do login if we have Un-trust AppLink 
     */
    public void doOAuthenticate() { 
        getAuthenticationLink().click();
        
        boolean isAuthenticateSuccess = false;
        //before any pop ups are open
        String parentHandle = driver.getWindowHandle();
        //after you have pop ups
        for (String popUpHandle : driver.getWindowHandles()) {
            if(!popUpHandle.equals(parentHandle)){
                driver.switchTo().window(popUpHandle);
                // finding oauthentication page. Note we must login with JIRA first
                if(driver.getCurrentUrl().contains(OAUTH_URL)){
                    JiraAuthenticationPage authenticationPage = pageBinder.bind(JiraAuthenticationPage.class);
                    isAuthenticateSuccess = authenticationPage.doApprove();
                }
            }
        }
        
        if (!isAuthenticateSuccess){
            Assert.fail("Cannot do authentication on AppLink");
        }
        
        // switch back to main page
        driver.switchTo().window(parentHandle);
    }
    
    /**
     * Check whether we have warning on IFrame or not
     * 
     * @return boolean
     */
    public boolean hasWarningOnIframe(){
        return getFrameWarningMsg(new Function<WebElement, Boolean>() {

            @Override
            public Boolean apply(WebElement element) {
                return element.isDisplayed();
            }
        });
    }
    
    public boolean hasWarningValWidth()
    {
        Poller.waitUntilTrue("warning valide Width is not visible", find(".width-error").timed().isVisible());
        return driver.findElement(By.cssSelector(".width-error")).isDisplayed();
    }
    
    /**
     * return createdandresolve image web element
     * 
     * @return an instance of WebElement which represent created vs resolved image
     */
    protected <R> R getCreatedVsResolvedImageWrapper(Function<WebElement, R> checker){
        return JiraChartHelper.getElementOnFrame(By.cssSelector("div.wiki-content div.jira-chart-macro-wrapper"), checker, driver);
    }
    
    /**
     * return createdandresolve image web element
     * 
     * @return an instance of WebElement which represent created vs resolved image
     */
    protected <R> R getCreatedVsResolvedImage(Function<WebElement, R> checker){
        return JiraChartHelper.getElementOnFrame(By.cssSelector("div.wiki-content div img"), checker, driver);
    }
    
    /**
     * return Waring message box inside IFrame 
     * 
     * @return
     */
    protected <R> R getFrameWarningMsg(Function<WebElement, R> checker){
        return JiraChartHelper.getElementOnFrame(By.cssSelector("div.aui-message-container div.aui-message.warning"), checker, driver);
    }
    
    public PageElement getAuthenticationLink() {
        return authenticationLink;
    }

    public void setAuthenticationLink(PageElement authenticationLink) {
        this.authenticationLink = authenticationLink;
    }

    @Override
    public PageElement getPanelBodyDialog()
    {
        return find(CSS_JIRA_CHART_CONTENT_CREATEDVSRESOLVED);
    }
}
