package com.atlassian.confluence.plugins.jiracharts.service;

/**
 * Created by khoa.pham on 4/11/14.
 */
public class CreatedAndResolvedChart implements JiraChartService
{
    @Override
    public String getChartName()
    {
        return "CREATED";
    }
}
