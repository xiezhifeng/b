package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.TwoDimensionalChartModel;
import com.atlassian.confluence.web.UrlBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class TwoDimensionalChart extends JiraHtmlChart
{

    private static final String[] chartParameters = new String[]{"xstattype", "ystattype", "showTotals", "sortDirection", "sortBy", "numberToShow"};
    private static final String CHART_WIDTH_DEFAULT = "590";

    public TwoDimensionalChart(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> twoDimensionalContextMap = JiraChartHelper.getCommonChartContext(parameters, result, context);

        String jql = parameters.get(JiraChartHelper.PARAM_JQL);
        String width = parameters.get(JiraChartHelper.PARAM_WIDTH);
        if(StringUtils.isBlank(width))
        {
            width = CHART_WIDTH_DEFAULT;
        }

        UrlBuilder urlBuilder = JiraChartHelper.getCommonJiraGadgetUrl(jql, width, getJiraGadgetRestUrl());
        JiraChartHelper.addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        TwoDimensionalChartModel chart = (TwoDimensionalChartModel) getChartModel(parameters.get(JiraChartHelper.PARAM_SERVER_ID), urlBuilder.toString());
        twoDimensionalContextMap.put("chartModel", chart);
        twoDimensionalContextMap.put(JiraChartHelper.PARAM_WIDTH, width + "px");
        return twoDimensionalContextMap;
    }

    @Override
    public Class<TwoDimensionalChartModel> getChartModelClass()
    {
        return TwoDimensionalChartModel.class;
    }

    @Override
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        return "/download/resources/confluence.extra.jira/jirachart_images/twodimensional-chart-placeholder.png";
    }

    @Override
    public String getJiraGadgetRestUrl()
    {
        return "/rest/gadget/1.0/twodimensionalfilterstats/generate?filterId=jql-";
    }

    @Override
    public String getTemplateFileName()
    {
        return "two-dimensional-chart.vm";
    }

    @Override
    public String[] getChartParameters()
    {
        return chartParameters;
    }
}
