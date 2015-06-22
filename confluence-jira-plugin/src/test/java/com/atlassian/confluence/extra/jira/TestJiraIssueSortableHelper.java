package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Ignore;

import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;

@Ignore
public class TestJiraIssueSortableHelper extends TestCase
{

    public void testSortReoderColumnsNotExistSortColumnInJQL()
    {
        String order = "ASC";
        String columnKey = "summay";
        String orderColumns = "";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE + " " + order;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns));
    }

    public void testSortReoderColumnsExistSortColumnInJQL()
    {
        String order = "ASC";
        String columnKey = "summary";
        String orderColumns = "summary";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE +" " + order;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns));
    }

    public void testSortReoderColumnsExistSortColumnInJQLHas2Columns()
    {
        String order = "ASC";
        String columnKey = "assignee";
        String orderColumns = "summary,type";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE + " " + order + "," + orderColumns;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns));
    }

    public void testSortReoderColumnsExistSortColumnInJQLHas3Columns()
    {
        String order = "ASC";
        String columnKey = "summary";
        String orderColumns = "summary,type,assignee";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE + " " + order + ",type,assignee";
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns));
    }

}
