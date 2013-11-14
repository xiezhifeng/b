package com.atlassian.confluence.plugins.jiracharts.model;

public enum ChartType
{
    PIE_CHART("pie", "/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-");

    private String jiraChartUrl;
    private String name;

    ChartType(String name, String jiraChartUrl)
    {
        this.jiraChartUrl = jiraChartUrl;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getJiraChartUrl()
    {
        return jiraChartUrl;
    }
}
