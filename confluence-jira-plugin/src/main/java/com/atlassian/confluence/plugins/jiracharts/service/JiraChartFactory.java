package com.atlassian.confluence.plugins.jiracharts.service;

/**
 * Created by khoa.pham on 4/11/14.
 */
public interface JiraChartFactory
{
    JiraChartService getJiraChartService(String chartType);
}
