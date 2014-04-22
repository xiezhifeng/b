package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.JiraGadgetService;
import com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.web.UrlBuilder;

import java.util.Map;

public class TwoDimensionalChartRenderer implements JiraChart
{

    private static final String[] chartParameters = new String[]{"xstattype", "ystattype", "showTotals", "sortDirection", "sortBy", "numberToShow"};

    private JiraGadgetService jiraGadgetService;

    public TwoDimensionalChartRenderer(JiraGadgetService jiraGadgetService)
    {
        this.jiraGadgetService = jiraGadgetService;
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> map = JiraChartHelper.getCommonChartContext(parameters, result, context);

        String jql = parameters.get(JiraChartHelper.PARAM_JQL);
        String width = parameters.get(JiraChartHelper.PARAM_WIDTH);

        UrlBuilder urlBuilder = JiraChartHelper.getCommonJiraGadgetUrl(jql, width, getJiraGadgetRestUrl());
        JiraChartHelper.addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        try
        {
            map.put("chartModel", jiraGadgetService.requestRestGadget(parameters.get(JiraChartHelper.PARAM_SERVER_ID), urlBuilder.toString()));
        }
        catch (Exception e)
        {

        }

        return map;
    }

    @Override
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        return null;
    }

    @Override
    public String getJiraGadgetRestUrl()
    {
        return "rest/gadget/1.0/twodimensionalfilterstats/generate?filterId=jql-";
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
