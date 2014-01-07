package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

import org.junit.Assert;

public class TestJiraIssueSortableHelper extends TestCase
{
    
    public void testSortColumnIsExistInJQL()
    {
        String columnName = "Summary";
        String columnKey = "summary";
        String orderColumns = "summary, type";
        Assert.assertEquals(columnKey, JiraIssueSortableHelper.checkOrderColumnExistJQL(columnName, columnKey, orderColumns));
    }
    
    public void testSortColumnIsNotExistInJQL()
    {
        String columnName = "Assignee";
        String columnKey = "assignee";
        String orderColumns = "summary, type";
        Assert.assertEquals("", JiraIssueSortableHelper.checkOrderColumnExistJQL(columnName, columnKey, orderColumns));
    }
    
    public void testSortReoderColumnsNotExistSortColumnInJQL()
    {
        String order = "ASC";
        String columnKey = "summay";
        String existColumn = "";
        String orderColumns = "";
        String expected = " \"" + columnKey + "\" " + order;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, existColumn, orderColumns));
    }
    
    public void testSortReoderColumnsExistSortColumnInJQL()
    {
        String order = "ASC";
        String columnKey = "summary";
        String existColumn = "summary";
        String orderColumns = "summary";
        String expected = " \"" + columnKey + "\" " + order;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, existColumn, orderColumns));
    }

    public void testSortReoderColumnsExistSortColumnInJQLHas2Columns()
    {
        String order = "ASC";
        String columnKey = "assignee";
        String existColumn = "";
        String orderColumns = "summary,type";
        String expected = " \"" + columnKey + "\" " + order + "," + orderColumns;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, existColumn, orderColumns));
    }
    
    public void testSortReoderColumnsExistSortColumnInJQLHas3Columns()
    {
        String order = "ASC";
        String columnKey = "summary";
        String existColumn = "";
        String orderColumns = "summary,type,assignee";
        String expected = " \"" + columnKey + "\" " + order + ",type,assignee";
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, existColumn, orderColumns));
    }

}
