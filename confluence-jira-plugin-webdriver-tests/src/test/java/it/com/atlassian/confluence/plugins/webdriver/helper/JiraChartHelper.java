package it.com.atlassian.confluence.plugins.webdriver.helper;

import com.atlassian.webdriver.AtlassianWebDriver;
import com.google.common.base.Function;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class JiraChartHelper
{

    /**
     * Utility method which helps to select element on IFrame
     *
     * @param elementBy
     * @return found element
     */
    public static  <R> R getElementOnFrame(final By elementBy, final Function<WebElement, R> checker, AtlassianWebDriver driver)
    {
        return changeToFrameContext(new Function<WebDriverWait, R>()
        {

            @Override
            public R apply(WebDriverWait innerWaiter)
            {
                WebElement returnElement = innerWaiter.until(ExpectedConditions
                        .presenceOfElementLocated(elementBy));

                return checker.apply(returnElement);
            }
        }, driver);
    }

    /**
     * Utility to change context from current page to IFrame
     *
     * @param runner abtract functionality which need to be run on IFrame context
     * @return the result of runner
     */
    public static  <R> R changeToFrameContext(Function<WebDriverWait, R> runner, AtlassianWebDriver driver)
    {
        String parentPage = driver.getWindowHandle();
        try
        {
            // switch to internal frame
            WebDriverWait waiter = new WebDriverWait(driver, 10);
            WebElement iFrame = waiter.until(ExpectedConditions
                    .visibilityOfElementLocated(By.cssSelector("iframe#chart-preview-iframe")));
            driver.switchTo().frame(iFrame);

            return runner.apply(waiter);
        } finally
        {
            //switch back to main page
            driver.switchTo().window(parentPage);
        }
    }
}
