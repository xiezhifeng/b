package com.atlassian.confluence.plugins.jiracharts.render;

public interface JiraChartFactory
{
    JiraChartRenderer getJiraChartRenderer(String chartType);
}
