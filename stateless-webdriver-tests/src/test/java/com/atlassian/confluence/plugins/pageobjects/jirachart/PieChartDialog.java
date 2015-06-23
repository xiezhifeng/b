package com.atlassian.confluence.plugins.pageobjects.jirachart;

import com.atlassian.confluence.plugins.helper.JiraChartHelper;
import com.atlassian.confluence.plugins.pageobjects.JiraAuthenticationPage;
import com.atlassian.confluence.plugins.pageobjects.JiraIssueMacroDialog;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraIssueFilterDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.TimedElement;
import com.atlassian.pageobjects.elements.query.Poller;

import com.google.common.base.Function;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import it.com.atlassian.confluence.plugins.webdriver.jiracharts.JiraChartTest;


public class PieChartDialog extends JiraChartDialog
{
    protected static final String OAUTH_URL = "/jira/plugins/servlet/oauth/authorize";
    protected static final String BORDER_CSS_CLASS_NAME = "jirachart-border";
    protected static final String JIRA_NAV_URL = "/jira/secure/IssueNavigator.jspa";

    @ElementBy(id = "macro-jirachart")
    protected PageElement clickToJiraChart;
    
    @ElementBy(id = "jira-chart-search-input")
    protected PageElement jqlSearch;
    
    @ElementBy(id = "jira-pie-chart-show-border")
    protected PageElement borderImage;
    
    @ElementBy(id = "jira-pie-chart-show-infor")
    protected PageElement showInfo;
    
    @ElementBy(className = "oauth-init")
    protected PageElement authenticationLink;
    
    @ElementBy(cssSelector = "#jira-chart-content-pie #jira-chart-width")
    protected PageElement pieChartWidth;

    @ElementBy(cssSelector = "#jira-chart .dialog-title")
    protected PageElement dialogTitle;

    @ElementBy(cssSelector = "#open-jira-issue-dialog")
    protected PageElement jiraIssuesMacroAnchor;

    @ElementBy(cssSelector = "#jira-chart .dialog-page-menu")
    protected PageElement dialogPageMenu;

    @ElementBy(id = "jira-chart-content-createdvsresolved")
    private PageElement jiraCreatedVsResolvedChart;
    
    @ElementBy(id = "jira-chart-statType")
    protected SelectElement statType;

    @ElementBy(id = "jira-chart-content-twodimensional")
    protected PageElement jiraTwoDimensionalChart;

    public PieChartDialog()
    {
        super("jira-chart");
    }

    @Init
    public void bind()
    {
        waitUntilVisible();
    }
    
    public PieChartDialog open()
    {
        clickToJiraChart.click();
        return this;
    }

    public PieChartDialog inputJqlSearch(String val)
    {
        jqlSearch.clear().type(val);
        jqlSearch.javascript().execute("jQuery(arguments[0]).trigger(\"change\")");
        return this;
    }
    
    public PieChartDialog pasteJqlSearch(String val)
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
    
    public TimedElement getPageEleJQLSearch()
    {
        return jqlSearch.timed();
    }
    
    public void clickBorderImage()
    {
        borderImage.click();
    }
    
    public void clickShowInforCheckbox()
    {
        Poller.waitUntilTrue(showInfo.timed().isVisible());
        showInfo.click();
    }
    
    public void setValueWidthColumn(String val)
    {
        pieChartWidth.clear().type(val);
    }

    public String getSelectedStatType()
    {
        return statType.getSelected().value();
    }

    public PageElement getJiraCreatedVsResolvedChart()
    {
        return jiraCreatedVsResolvedChart;
    }

    public PageElement getJiraTwoDimensionalChart()
    {
        return jiraTwoDimensionalChart;
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
        return getPieImage(new Function<WebElement, Boolean>()
        {

            @Override
            public Boolean apply(WebElement pieImage)
            {
                // Note : currently don't know why image cannot display during testing session. Show will use 'src' attribute to check
                String imageSrc = pieImage.getAttribute("src");
                return imageSrc.contains(JiraChartTest.JIRA_CHART_BASE_64_PREFIX);
            }
        });
    }
    
    public boolean hadBorderImageInDialog()
    {
        return getPieImageWrapper(new Function<WebElement, Boolean>()
        {

            @Override
            public Boolean apply(WebElement pieImageWrapper)
            {
                String apppliedCSSClass = pieImageWrapper.getAttribute("class");
                return apppliedCSSClass.contains(BORDER_CSS_CLASS_NAME);
            }
        });
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

        Assert.assertTrue("Authenticate application link", isAuthenticateSuccess);

        // switch back to main page
        driver.switchTo().window(parentHandle);
    }
    
    /**
     * Check whether we have warning on IFrame or not
     * 
     * @return boolean
     */
    public boolean hasWarningOnIframe(){
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
        Poller.waitUntilTrue("warning valide Width is not visible", find(".width-error").timed().isVisible());
        return driver.findElement(By.cssSelector(".width-error")).isDisplayed();
    }
    
    /**
     * return pie image web element
     * 
     * @return an instance of WebElement which represent pie image
     */
    private <R> R getPieImageWrapper(Function<WebElement, R> checker){
        return JiraChartHelper.getElementOnFrame(By.cssSelector("div.wiki-content div.jira-chart-macro-wrapper"), checker, driver);
    }
    
    /**
     * return pie image web element
     * 
     * @return an instance of WebElement which represent pie image
     */
    private <R> R getPieImage(Function<WebElement, R> checker){
        return JiraChartHelper.getElementOnFrame(By.cssSelector("div.wiki-content div img"), checker, driver);
    }
    
    /**
     * return Waring message box inside IFrame 
     * 
     * @return
     */
    private <R> R getFrameWarningMsg(Function<WebElement, R> checker){
        return JiraChartHelper.getElementOnFrame(By.className("aui-message"), checker, driver);
    }
    
    public PageElement getAuthenticationLink() {
        return authenticationLink;
    }

    public void setAuthenticationLink(PageElement authenticationLink) {
        this.authenticationLink = authenticationLink;
    }

    public PageElement getJiraIssuesMacroAnchor()
    {
        return jiraIssuesMacroAnchor;
    }

    public JiraIssueFilterDialog clickJiraIssuesMacroAnchor()
    {
        jiraIssuesMacroAnchor.click();

        JiraIssueFilterDialog jiraIssueFilterDialog = this.pageBinder.bind(JiraIssueFilterDialog.class);
        Poller.waitUntilTrue(jiraIssueFilterDialog.isVisibleTimed());

        return jiraIssueFilterDialog;
    }

    public Dialog selectChartDialog(Class<? extends Dialog> chartClass, PageElement chartElement, String chartText)
    {
        Poller.waitUntilTrue(dialogPageMenu.timed().isVisible());
        for (PageElement chartType : dialogPageMenu.findAll(By.cssSelector(".item-button")))
        {
            if (chartType.getText().equalsIgnoreCase(chartText))
            {
                chartType.click();
                Poller.waitUntilTrue(chartElement.timed().isVisible());
                return this.pageBinder.bind(chartClass);
            }
        }
        return null;
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
}
