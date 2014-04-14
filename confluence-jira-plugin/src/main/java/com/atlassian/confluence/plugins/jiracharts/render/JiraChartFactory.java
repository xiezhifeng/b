package com.atlassian.confluence.plugins.jiracharts.render;

public interface JiraChartFactory
{
    JiraChart getJiraChartRenderer(String chartType);
}
