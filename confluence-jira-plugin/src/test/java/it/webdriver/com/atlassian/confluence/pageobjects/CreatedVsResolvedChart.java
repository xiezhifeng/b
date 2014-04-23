package it.webdriver.com.atlassian.confluence.pageobjects;


import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Option;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.google.common.base.Function;
import com.ibm.icu.impl.Assert;
import it.webdriver.com.atlassian.confluence.jiracharts.JiraChartWebDriverTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CreatedVsResolvedChart extends Dialog
{
    private static final String OAUTH_URL = "/jira/plugins/servlet/oauth/authorize";

    private static final String BORDER_CSS_CLASS_NAME = "jirachart-border";

    private static final String JIRA_NAV_URL = "/jira/secure/IssueNavigator.jspa";

    @ElementBy(className = "insert-jira-chart-macro-button")
    private PageElement insertMacroBtn;

    @ElementBy(id = "jira-chart-search-input")
    private PageElement jqlSearch;

    @ElementBy(id = "jira-chart-border")
    private PageElement borderImage;

    @ElementBy(id = "jira-chart-show-infor")
    private PageElement showInfo;

    @ElementBy(className = "oauth-init")
    private PageElement authenticationLink;

    @ElementBy(id = "jira-chart-width")
    private PageElement width;

    @ElementBy(cssSelector = "#periodName")
    private SelectElement periodName;

    @ElementBy(cssSelector = "#daysprevious")
    private PageElement daysPrevious;

    @ElementBy(cssSelector = "#cumulative")
    private PageElement cumulative;

    @ElementBy(cssSelector = "#versionLabel")
    private SelectElement versionLabel;

    @ElementBy(cssSelector = "#showunresolvedtrend")
    private PageElement showUnResolvedTrend;

    @ElementBy(cssSelector = ".days-previous-error")
    private PageElement daysPreviousError;

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

    public void setSelectedForPeriodName(Option option)
    {
        periodName.select(option);
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
        daysPrevious.type(value);
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
        driver.findElement(By.cssSelector("#jira-chart-search-button")).click();
    }

    public PageElement getPageEleJQLSearch()
    {
        return jqlSearch;
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
        return getPieImage(new Function<WebElement, Boolean>()
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
        return getPieImage(new Function<WebElement, Boolean>() {

            @Override
            public Boolean apply(WebElement pieImage) {
                // Note : currently don't know why image cannot display during testing session. Show will use 'src' attribute to check
                String imageSrc = pieImage.getAttribute("src");
                return imageSrc.contains(JiraChartWebDriverTest.JIRA_CHART_PROXY_SERVLET);
            }
        });
    }

    public boolean hadBorderImageInDialog()
    {
        return getPieImageWrapper(new Function<WebElement, Boolean>(){

            @Override
            public Boolean apply(WebElement pieImageWrapper) {
                String apppliedCSSClass = pieImageWrapper.getAttribute("class");
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


    /**
     * return pie image web element
     *
     * @return an instance of WebElement which represent pie image
     */
    private <R> R getPieImageWrapper(Function<WebElement, R> checker){
        return getElementOnFrame(By.cssSelector("div.wiki-content div.jira-chart-macro-wrapper"), checker);
    }

    /**
     * return pie image web element
     *
     * @return an instance of WebElement which represent pie image
     */
    private <R> R getPieImage(Function<WebElement, R> checker){
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

    public PageElement getAuthenticationLink() {
        return authenticationLink;
    }

    public void setAuthenticationLink(PageElement authenticationLink) {
        this.authenticationLink = authenticationLink;
    }
}
