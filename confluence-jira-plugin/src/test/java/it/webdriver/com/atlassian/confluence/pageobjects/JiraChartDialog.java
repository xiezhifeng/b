package it.webdriver.com.atlassian.confluence.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Function;
import com.ibm.icu.impl.Assert;

public class JiraChartDialog extends Dialog
{
    private static final String OAUTH_URL = "/jira/plugins/servlet/oauth/authorize";
    
    private static final String IMAGE_PROXY_SERVLET = "/confluence/plugins/servlet/jira-chart-proxy";
    
    private static final String BORDER_CSS_CLASS_NAME = "jirachart-border";
    
    @ElementBy(id = "macro-jirachart")
    private PageElement clickToJiraChart;
    
    @ElementBy(id = "jira-chart-inputsearch")
    private PageElement jqlSearch;
    
    @ElementBy(id = "jira-chart-border")
    private PageElement borderImage;
    
    @ElementBy(className = "oauth-init")
    private PageElement authenticationLink;
    
    public JiraChartDialog()
    {
        super("jira-chart");
    }
    
    public JiraChartDialog open()
    {
        clickToJiraChart.click();
        return this;
    }

    public String getTitleDialog()
    {
        return driver.findElement(By.cssSelector("#jira-chart .dialog-title")).getText();
    }
    
    public JiraChartDialog inputJqlSearch(String val)
    {
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }
    
    public JiraChartDialog pasteJqlSearch(String val)
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
        driver.findElement(By.cssSelector("#jira-chart .jira-chart-search button")).click();
    }
    
    public PageElement getPageEleJQLSearch()
    {
        return jqlSearch;
    }
    
    public void clickBorderImage()
    {
        borderImage.click();
    }
    
    @SuppressWarnings("deprecation")
    public boolean hadImageInDialog()
    {
        return getPieImage(new Function<WebElement, Boolean>() {

            @Override
            public Boolean apply(WebElement pieImage) {
             // Note : currently don't know why image cannot display during testing session. Show will use 'src' attribute to check
                String imageSrc = pieImage.getAttribute("src");
                return imageSrc.contains(IMAGE_PROXY_SERVLET);
            }
        });
    }
    
    @SuppressWarnings("deprecation")
    public boolean hadBorderImageInDialog()
    {
        return getPieImage(new Function<WebElement, Boolean>() {

            @Override
            public Boolean apply(WebElement pieImage) {
                String apppliedCSSClass = pieImage.getAttribute("class");
                return apppliedCSSClass.contains(BORDER_CSS_CLASS_NAME);
            }
        });
    }
    
    public EditContentPage clickInsertDialog()
    {
        clickButton("insert-jira-chart-macro-button", false);
        return pageBinder.bind(EditContentPage.class);
    }
    
    public String getLinkMoreToCome()
    {
        return driver.findElement(By.cssSelector("#jira-chart .dialog-page-menu .moreToCome a")).getAttribute("href");
    }
    
    public boolean needAuthentication(){
        WebElement element = ((WebElement) driver.executeScript("return jQuery('.jira-chart-search .oauth-init')[0];"));
        return element != null;
    }
    
    /**
     *  Do login if we have Un-trust AppLink 
     */
    public void doOAuthenticate() { 
        authenticationLink.click();
        
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
    
    /**
     * return pie image web element
     * 
     * @return an instance of WebElement which represent pie image
     */
    private <R> R getPieImage(Function<WebElement, R> checker){
        return getElementOnFrame(By.cssSelector("div.wiki-content span img"), checker);
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
                    .visibilityOfElementLocated(By.cssSelector("iframe#macro-preview-iframe")));
            driver.switchTo().frame(iFrame);
            
            return runner.apply(waiter);
        } finally {
            //switch back to man page
            driver.switchTo().window(parentPage);
        }
    }
}
