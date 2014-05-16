package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
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
    private ApplicationLinkService applicationLinkService;

    @Mock
    private ApplicationLink applicationLink;

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

    private Map<String, Object> expectedMap;

    private String requestUrl;

    @Before
    public void init() throws Exception
    {
        jiraChart = new TwoDimensionalChart(applicationLinkService);

        requestUrl = "/rest/gadget/1.0/twodimensionalfilterstats/generate?filterId=jql-status%3Dopen";

        parameters = new HashMap<String, String>();
        parameters.put("chartType", "twodimensional");
        parameters.put("border", "false");
        parameters.put("isAuthenticated", "true");
        parameters.put("jql", "status=open");
        parameters.put("serverId", "8835b6b9-5676-3de4-ad59-bbe987416662");


        expectedMap = new HashMap<String, Object>();
        expectedMap.put("showBorder", false);
        expectedMap.put("showInfor", false);
        expectedMap.put("isPreviewMode", false);
        expectedMap.put("jqlValidationResult", result);
        expectedMap.put("chartModel", null);

        PowerMockito.mockStatic(MacroUtils.class);
        when(MacroUtils.defaultVelocityContext()).thenReturn(new HashMap<String, Object>());
        when(applicationLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(applicationLink);

        when(applicationLink.getDisplayUrl()).thenReturn(new URI("confluence"));
        when(applicationLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);

        when(requestFactory.createRequest(Request.MethodType.GET, "confluence" + requestUrl)).thenReturn(request);


    }

    @Test
    public void testSetupContext() throws Exception
    {
        when(request.execute()).thenReturn("");
        Map<String, Object> map = jiraChart.setupContext(parameters, result, conversionContext);
        Assert.assertEquals(expectedMap, map);
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
}
