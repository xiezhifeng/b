package com.atlassian.confluence.plugins.jiracharts.model;

import com.atlassian.confluence.extra.jira.model.Locatable;
import com.atlassian.confluence.extra.jira.model.PieChartModel;

import java.util.HashMap;
import java.util.Map;

public enum ChartType
{
    PIE_CHART("pie", "/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-", PieChartModel.class);

    private String jiraChartUrl;
    private String name;
    private Class<? extends Locatable> modelClass;
    private static Map<String, ChartType> chartTypeMap = new HashMap<String, ChartType>();
    static
    {
        chartTypeMap.put(PIE_CHART.getName(), PIE_CHART);
    }

    ChartType(String name, String jiraChartUrl, Class<?extends Locatable> modelClass)
    {
        this.jiraChartUrl = jiraChartUrl;
        this.name = name;
        this.modelClass = modelClass;
    }

    public String getName()
    {
        return name;
    }

    public String getJiraChartUrl()
    {
        return jiraChartUrl;
    }

    public Class<? extends Locatable> getModelClass()
    {
        return modelClass;
    }

    public static ChartType getChartType(String name)
    {
        ChartType chartType = chartTypeMap.get(name);

        //Get default chart if can not find chart
        return chartType != null ? chartType : PIE_CHART;
    }
}
