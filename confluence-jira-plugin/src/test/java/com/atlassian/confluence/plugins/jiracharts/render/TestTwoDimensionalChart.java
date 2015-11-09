package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.TwoDimensionalChartModel;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.sal.api.net.Request;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MacroUtils.class)
public class TestTwoDimensionalChart
{

    @Mock
    private ReadOnlyApplicationLinkService applicationLinkService;

    @Mock
    private ReadOnlyApplicationLink applicationLink;

    @Mock
    private ApplicationLinkRequestFactory requestFactory;

    @Mock
    private ApplicationLinkRequest request;

    @Mock
    private JQLValidationResult result;

    @Mock
    private ConversionContext conversionContext;

    private JiraHtmlChart jiraChart;

    private Map<String, String> parameters;

    private String requestUrl;

    @Before
    public void init() throws Exception
    {
        jiraChart = new TwoDimensionalChart(applicationLinkService, null, null);

        requestUrl = "/rest/gadget/1.0/twodimensionalfilterstats/generate?filterId=jql-status%3Dopen&sortBy=natural&showTotals=true&numberToShow=9999&sortDirection=asc";

        parameters = new HashMap<String, String>();
        parameters.put("chartType", "twodimensional");
        parameters.put("isAuthenticated", "true");
        parameters.put("jql", "status=open");
        parameters.put("serverId", "8835b6b9-5676-3de4-ad59-bbe987416662");

        PowerMockito.mockStatic(MacroUtils.class);
        when(MacroUtils.defaultVelocityContext()).thenReturn(new HashMap<String, Object>());
        when(applicationLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(applicationLink);

        when(applicationLink.getRpcUrl()).thenReturn(new URI("confluence"));
        when(applicationLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);

        when(requestFactory.createRequest(Request.MethodType.GET, "confluence" + requestUrl)).thenReturn(request);


    }

    @Test
    public void testSetupContext() throws Exception
    {
        when(request.execute()).thenReturn("{\"totalRows\": 2}");
        Map<String, Object> map = jiraChart.setupContext(parameters, result, conversionContext);
        Assert.assertEquals(map.get("numberRowShow"), "2");
        Assert.assertNotNull(map.get("chartModel"));
    }

    @Test
    public void testGetChartModel() throws Exception
    {

        when(request.execute()).thenReturn("{ " +
                    "rows: [{cells: [{markup: \"markup1\", classes: [\"class1\"]}]}], " +
                    "firstRow: {cells: [{markup: \"markup2\", classes: [\"class2\"]}]}, " +
                    "xHeading: \"xHeading\"," +
                    "yHeading: \"yHeading\"" +
                "}");
        TwoDimensionalChartModel actualChartModel = (TwoDimensionalChartModel) jiraChart.getChartModel("8835b6b9-5676-3de4-ad59-bbe987416662", requestUrl);

        Assert.assertEquals(actualChartModel.getxHeading(), "xHeading");
        Assert.assertEquals(actualChartModel.getyHeading(), "yHeading");

        TwoDimensionalChartModel.Row row = actualChartModel.getFirstRow();
        TwoDimensionalChartModel.Cell cell = row.getCells().get(0);
        Assert.assertEquals(cell.getMarkup(), "markup2");
        Assert.assertArrayEquals(cell.getClasses(), new String[]{"class2"});

        row = actualChartModel.getRows().get(0);
        cell = row.getCells().get(0);
        Assert.assertEquals(cell.getMarkup(), "markup1");
        Assert.assertArrayEquals(cell.getClasses(), new String[]{"class1"});

    }

    @Test
    public void testMarkup() throws Exception
    {
        TwoDimensionalChart chart = new TwoDimensionalChart(null, null, null);
        Class[] argTypes = new Class[] { String.class, String.class };
        Method method = TwoDimensionalChart.class.getDeclaredMethod("getStatusMarkup", argTypes);
        method.setAccessible(true);
        Assert.assertEquals(method.invoke(chart, "<img src=\"http://jira.com/image/open.png\" width=\"10\">", "http://localhost:8080"), "<img src=\"http://jira.com/image/open.png\" width=\"10\">");
        Assert.assertEquals(method.invoke(chart, "<img src=\"/jira/image/open.png\" width=\"10\">", "http://localhost:8080"), "<img src=\"http://localhost:8080/jira/image/open.png\" width=\"10\">");
        Assert.assertEquals(method.invoke(chart, "Not a image", "http://localhost:8080"), "Not a image");
    }


}
