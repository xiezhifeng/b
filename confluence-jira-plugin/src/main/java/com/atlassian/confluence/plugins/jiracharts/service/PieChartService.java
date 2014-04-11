package com.atlassian.confluence.plugins.jiracharts.service;

/**
 * Created by khoa.pham on 4/11/14.
 */
public class PieChartService implements JiraChartService
{
    @Override
    public String getChartName()
    {
        return "PIE";
    }
}
