package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

import org.junit.Assert;

import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;

public class TestJiraIssueSortableHelper extends TestCase
{

    public void testSortReoderColumnsNotExistSortColumnInJQL()
    {
        String order = "ASC";
        String columnKey = "summay";
        String orderColumns = "";
        String expected = columnKey + " " + order;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns));
    }

    public void testSortReoderColumnsExistSortColumnInJQL()
    {
        String order = "ASC";
        String columnKey = "summary";
        String orderColumns = "summary";
        String expected = columnKey + " " + order;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns));
    }

    public void testSortReoderColumnsExistSortColumnInJQLHas2Columns()
    {
        String order = "ASC";
        String columnKey = "assignee";
        String orderColumns = "summary,type";
        String expected = columnKey + " " + order + "," + orderColumns;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns));
    }

    public void testSortReoderColumnsExistSortColumnInJQLHas3Columns()
    {
        String order = "ASC";
        String columnKey = "summary";
        String orderColumns = "summary,type,assignee";
        String expected = columnKey + " " + order + ",type,assignee";
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns));
    }

}
