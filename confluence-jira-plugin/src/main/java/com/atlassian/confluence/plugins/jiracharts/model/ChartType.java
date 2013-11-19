package com.atlassian.confluence.plugins.jiracharts.model;

import com.atlassian.confluence.extra.jira.model.Locatable;
import com.atlassian.confluence.extra.jira.model.PieChartModel;

public enum ChartType
{
    PIE_CHART("pie", "/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-", PieChartModel.class);

    private String jiraChartUrl;
    private String name;
    private Class<? extends Locatable> modelClass;

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
        ChartType[] chartTypes = ChartType.values();
        for (ChartType chartType : chartTypes)
        {
            if(chartType.getName().equals(name))
            {
                return chartType;
            }
        }

        //Get default chart if can not find chart
        return PIE_CHART;
    }
}
