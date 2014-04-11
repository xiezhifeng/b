package com.atlassian.confluence.plugins.jiracharts.render;

public interface JiraChartRendererFactory
{
    JiraChartRenderer getJiraChartRenderer(String chartType);
}
