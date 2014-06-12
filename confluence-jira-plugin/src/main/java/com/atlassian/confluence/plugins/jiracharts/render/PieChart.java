package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.JiraImageChartModel;
import com.atlassian.confluence.web.UrlBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.*;

public class PieChart extends JiraImageChart
{
    private static final String PARAM_STAT_TYPE = "statType";

    private static final String[] chartParameters = new String[]{PARAM_STAT_TYPE};

    private static final String DEFAULT_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jirachart_images/jirachart_placeholder.png";

    public PieChart(ContextPathHolder pathHolder, Base64JiraChartImageService base64JiraChartImageService)
    {
        this.base64JiraChartImageService = base64JiraChartImageService;
        this.pathHolder = pathHolder;
    }

    @Override
    public String[] getChartParameters()
    {
        return chartParameters;
    }

    @Override
    public String getTemplateFileName()
    {
        return "piechart.vm";
    }

    @Override
    public String getJiraGadgetRestUrl()
    {
        return "/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-";
    }

    @Override
    public String getJiraGadgetUrl(HttpServletRequest request)
    {
        UrlBuilder urlBuilder = getCommonJiraGadgetUrl(request.getParameter(PARAM_JQL), request.getParameter(PARAM_WIDTH), getJiraGadgetRestUrl());
        addJiraChartParameter(urlBuilder, request, getChartParameters());
        return urlBuilder.toString();
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {

        Map<String, Object> contextMap = getCommonChartContext(parameters, result, context);
        JiraImageChartModel chartModel = getImageSource(parameters);
        contextMap.put("srcImg", chartModel.getBase64Image());
        contextMap.put(PARAM_STAT_TYPE, chartModel.getStatType());
        return contextMap;
    }

    @Override
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        return urlBuilder.toString();
    }

    @Override
    public String getDefaultImagePlaceholderUrl()
    {
        return DEFAULT_PLACEHOLDER_IMG_PATH;
    }

    @Override
    public String getDefaultPDFChartWidth()
    {
        return "320";
    }
}
