package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.web.UrlBuilder;

import java.util.Map;

public class TwoDimensionalChart extends JiraHtmlChart
{

    private static final String[] chartParameters = new String[]{"xstattype", "ystattype", "showTotals", "sortDirection", "sortBy", "numberToShow"};

    public TwoDimensionalChart(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> map = JiraChartHelper.getCommonChartContext(parameters, result, context);

        String jql = parameters.get(JiraChartHelper.PARAM_JQL);
        String width = parameters.get(JiraChartHelper.PARAM_WIDTH);

        UrlBuilder urlBuilder = JiraChartHelper.getCommonJiraGadgetUrl(jql, width, getJiraGadgetRestUrl());
        JiraChartHelper.addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        com.atlassian.confluence.plugins.jiracharts.model.TwoDimensionalChart chart = (com.atlassian.confluence.plugins.jiracharts.model.TwoDimensionalChart) getChartModel(parameters.get(JiraChartHelper.PARAM_SERVER_ID), urlBuilder.toString());
        map.put("chartModel", chart);
        return map;
    }

    @Override
    public Class getChartModelClass()
    {
        return com.atlassian.confluence.plugins.jiracharts.model.TwoDimensionalChart.class;
    }

    @Override
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        //TODO: will set the default image for the chart
        return null;
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
