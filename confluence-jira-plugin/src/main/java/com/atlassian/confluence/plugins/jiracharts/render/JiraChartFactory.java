package com.atlassian.confluence.plugins.jiracharts.render;

/**
 * Chart factory to get the chart base on chart type
 * @since 5.5.2
 */
public interface JiraChartFactory
{
    /**
     * Get the jira chart base on chart type
     * @param chartType
     * @return jira chart
     */
    JiraChart getJiraChartRenderer(String chartType);
}





