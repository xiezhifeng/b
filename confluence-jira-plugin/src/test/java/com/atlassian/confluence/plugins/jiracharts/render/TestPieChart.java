package com.atlassian.confluence.plugins.jiracharts.render;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.confluence.plugins.jiracharts.model.JiraImageChartModel;
import com.atlassian.sal.api.net.ResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.web.UrlBuilder;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MacroUtils.class)
public class TestPieChart
{

    @Mock
    private ContextPathHolder pathHolder;

    @Mock
    private I18NBeanFactory i18NBeanFactory;

    @Mock
    private Base64JiraChartImageService base64JiraChartImageService;

    @Mock
    private JQLValidationResult result;

    @Mock
    private ConversionContext conversionContext;

    @Mock
    private I18NBean i18NBean;

    private JiraChart jiraChart;

    private Map<String, String> parameters;

    private Map<String, Object> expectedMap;

    @Before
    public void init()
    {
        jiraChart = new PieChart(pathHolder, base64JiraChartImageService);

        parameters = new HashMap<String, String>();
        parameters.put("chartType", "pie");
        parameters.put("statType", "statuses");
        parameters.put("showInfor", "false");
        parameters.put("border", "false");
        parameters.put("isAuthenticated", "true");
        parameters.put("jql", "status=open");
        parameters.put("serverId", "8835b6b9-5676-3de4-ad59-bbe987416662");

        expectedMap = new HashMap<String, Object>();
        expectedMap.put("showBorder", false);
        expectedMap.put("showInfor", false);
        expectedMap.put("isPreviewMode", false);
        expectedMap.put("statType", "statuses");
        expectedMap.put("jqlValidationResult", result);
        expectedMap.put("srcImg", "test");

        PowerMockito.mockStatic(MacroUtils.class);
        when(MacroUtils.defaultVelocityContext()).thenReturn(new HashMap<String, Object>());

        when(i18NBeanFactory.getI18NBean()).thenReturn(i18NBean);
        when(i18NBean.getText(anyString())).thenReturn("statuses");

        when(pathHolder.getContextPath()).thenReturn("/confluence");

    }

    @Test
    public void testSetupContext() throws MacroExecutionException, ResponseException
    {
        JiraImageChartModel jiraImageChartModel = new JiraImageChartModel();
        jiraImageChartModel.setBase64Image("test");
        jiraImageChartModel.setStatType("statuses");

        when(base64JiraChartImageService.getBase64JiraChartImageModel(anyString(), anyString())).thenReturn(jiraImageChartModel);

        Map<String, Object> map = jiraChart.setupContext(parameters, result, conversionContext);
        Assert.assertEquals(expectedMap, map);
    }

    @Test
    public void testGetImagePlaceholderUrl()
    {
        UrlBuilder urlBuilder = new UrlBuilder("");
        Assert.assertEquals(jiraChart.getImagePlaceholderUrl(parameters, urlBuilder), "?statType=statuses");
    }

}
