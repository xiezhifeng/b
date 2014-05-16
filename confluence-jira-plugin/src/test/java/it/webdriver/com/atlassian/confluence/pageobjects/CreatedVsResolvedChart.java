package it.webdriver.com.atlassian.confluence.pageobjects;


import it.webdriver.com.atlassian.confluence.jiracharts.JiraChartWebDriverTest;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Option;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.base.Function;
import com.ibm.icu.impl.Assert;

public class CreatedVsResolvedChart extends Dialog
{
    private static final String OAUTH_URL = "/jira/plugins/servlet/oauth/authorize";
    
    private static final String BORDER_CSS_CLASS_NAME = "jirachart-border";
    
    private static final String JIRA_NAV_URL = "/jira/secure/IssueNavigator.jspa";
    
    @ElementBy(cssSelector = "#jira-chart-content-createdvsresolved #jira-chart-search-input")
    private SelectElement jqlSearch;
    
    @ElementBy(cssSelector = "#jira-chart-content-createdvsresolved #created-vs-resolved-chart-periodName")
    private SelectElement periodName;

    @ElementBy(cssSelector = "#jira-chart-content-createdvsresolved #created-vs-resolved-chart-daysprevious")
    private PageElement daysPrevious;

    @ElementBy(cssSelector = "#created-vs-resolved-chart-cumulative")
    private PageElement cumulative;

    @ElementBy(cssSelector = "#created-vs-resolved-chart-versionLabel")
    private SelectElement versionLabel;

    @ElementBy(cssSelector = "#created-vs-resolved-chart-showunresolvedtrend")
    private PageElement showUnResolvedTrend;

    @ElementBy(cssSelector = "#jira-chart-content-createdvsresolved .days-previous-error")
    private PageElement daysPreviousError;
    
    @ElementBy(cssSelector = "#jira-chart-content-createdvsresolved #jira-createdvsresolved-chart-show-border")
    private PageElement borderImage;
    
    @ElementBy(cssSelector = "#jira-chart-content-createdvsresolved #jira-createdvsresolved-chart-show-infor")
    private PageElement showInfo;
    
    @ElementBy(cssSelector = "#jira-chart-content-createdvsresolved #oauth-init")
    private PageElement authenticationLink;
    
    @ElementBy(cssSelector = "#jira-chart-content-createdvsresolved #jira-chart-width")
    private PageElement width;
    
    @ElementBy(cssSelector = ".insert-jira-chart-macro-button")
    private PageElement insertMacroBtn;

    public CreatedVsResolvedChart()
    {
        super("jira-chart");

    }

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
    
    public CreatedVsResolvedChart inputJqlSearch(String val)
    {
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }
    
    public CreatedVsResolvedChart pasteJqlSearch(String val)
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
        driver.findElement(By.cssSelector("#jira-chart-content-createdvsresolved #jira-chart-search-button")).click();
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
        return getCreatedVsResolvedImage(new Function<WebElement, Boolean>() {

            @Override
            public Boolean apply(WebElement createVsResolved) {
             // Note : currently don't know why image cannot display during testing session. Show will use 'src' attribute to check
                String imageSrc = createVsResolved.getAttribute("src");
                return imageSrc.contains(JiraChartWebDriverTest.JIRA_CHART_PROXY_SERVLET);
            }
        });
    }
    
    public boolean hadBorderImageInDialog()
    {
        return getCreatedVsResolvedImageWrapper(new Function<WebElement, Boolean>(){
            
            @Override
            public Boolean apply(WebElement createdVsResolvedImageWrapper) {
                String apppliedCSSClass = createdVsResolvedImageWrapper.getAttribute("class");
                return apppliedCSSClass.contains(BORDER_CSS_CLASS_NAME);
            }
        });
    }
    
    public EditContentPage clickInsertDialog()
    {
        // wait until insert button is available
        insertMacroBtn.timed().isEnabled();
        clickButton("insert-jira-chart-macro-button", true);
        
        return pageBinder.bind(EditContentPage.class);
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
     * return pie image web element
     * 
     * @return an instance of WebElement which represent created vs resolved image
     */
    private <R> R getCreatedVsResolvedImageWrapper(Function<WebElement, R> checker){
        return getElementOnFrame(By.cssSelector("div.wiki-content div.jira-chart-macro-wrapper"), checker);
    }
    
    /**
     * return pie image web element
     * 
     * @return an instance of WebElement which represent created vs resolved image
     */
    private <R> R getCreatedVsResolvedImage(Function<WebElement, R> checker){
        return getElementOnFrame(By.cssSelector("div.wiki-content div img"), checker);
    }
    
    /**
     * return Waring message box inside IFrame 
     * 
     * @return
     */
    private <R> R getFrameWarningMsg(Function<WebElement, R> checker){
        return getElementOnFrame(By.cssSelector("div.aui-message-container div.aui-message.warning"), checker);
    }
    
    /**
     * Utility method which helps to select element on IFrame
     * 
     * @param by
     * @return found element
     */
    private <R> R getElementOnFrame(final By by, final Function<WebElement, R> checker){
        return changeToFrameContext(new Function<WebDriverWait, R>() {

            @Override
            public R apply(WebDriverWait innerWaiter) {
                WebElement returnElement = innerWaiter.until(ExpectedConditions
                        .presenceOfElementLocated(by));
                
                return checker.apply(returnElement);
            }
        });
    }
    
    /**
     * Utility to change context from current page to IFrame
     * 
     * @param runner abtract functionality which need to be run on IFrame context
     * @return the result of runner
     */
    private <R> R changeToFrameContext(Function<WebDriverWait, R> runner){
        String parentPage = driver.getWindowHandle();
        try {
            // switch to internal frame
            WebDriverWait waiter = new WebDriverWait(driver, 10);
            WebElement iFrame = waiter.until(ExpectedConditions
                    .visibilityOfElementLocated(By.cssSelector("iframe#chart-preview-iframe")));
            driver.switchTo().frame(iFrame);
            
            return runner.apply(waiter);
        } finally {
            //switch back to man page
            driver.switchTo().window(parentPage);
        }
    }

    public PageElement getAuthenticationLink() {
        return authenticationLink;
    }

    public void setAuthenticationLink(PageElement authenticationLink) {
        this.authenticationLink = authenticationLink;
    }
    
}
