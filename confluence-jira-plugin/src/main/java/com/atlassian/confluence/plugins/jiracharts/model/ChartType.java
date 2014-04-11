package com.atlassian.confluence.plugins.jiracharts.model;

import java.util.*;

public enum ChartType
{
    PIE_CHART("pie", "piechart", Arrays.asList("statType"));

    private String jiraChartUrl;
    private String name;
    private List<String> extendedParams;

    private static Map<String, ChartType> chartTypeMap = new HashMap<String, ChartType>();
    static
    {
        chartTypeMap.put(PIE_CHART.getName(), PIE_CHART);
    }

    ChartType(String name, String jiraChartUrl, List<String> extendedParams)
    {
        this.jiraChartUrl = jiraChartUrl;
        this.name = name;
        this.extendedParams = extendedParams;
    }

    public String getName()
    {
        return name;
    }

    public String getJiraChartUrl()
    {
        return "/rest/gadget/1.0/" + jiraChartUrl +"/generate?projectOrFilterId=jql-";
    }

    public List<String> getExtendedParams()
    {
        return extendedParams;
    }

    public static ChartType getChartType(String name)
    {
        ChartType chartType = chartTypeMap.get(name);

        //Get default chart if can not find chart
        return chartType != null ? chartType : PIE_CHART;
    }
}
