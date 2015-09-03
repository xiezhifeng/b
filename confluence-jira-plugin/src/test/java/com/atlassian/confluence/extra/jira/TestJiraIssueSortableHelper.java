package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

import org.junit.Assert;

import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;
import java.util.Map;

public class TestJiraIssueSortableHelper extends TestCase
{

    public void testSortReoderColumnsNotExistSortColumnInJQL()
    {
        String order = "ASC";
        String columnKey = "summay";
        String orderColumns = "";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE + " " + order;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns, getJiraColumnInfo()));
    }

    public void testSortReoderColumnsExistSortColumnInJQL()
    {
        String order = "ASC";
        String columnKey = "summary";
        String orderColumns = "summary";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE +" " + order;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns, getJiraColumnInfo()));
    }

    public void testSortReoderColumnsExistSortColumnInJQLHas2Columns()
    {
        String order = "ASC";
        String columnKey = "assignee";
        String orderColumns = "summary,type";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE + " " + order + "," + orderColumns;
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns, getJiraColumnInfo()));
    }

    public void testSortReoderColumnsExistSortColumnInJQLHas3Columns()
    {
        String order = "ASC";
        String columnKey = "summary";
        String orderColumns = "summary,type,assignee";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE + " " + order + ",type,assignee";
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns, getJiraColumnInfo()));
    }

    public void testSortReoderColumnsExistWithAlias()
    {
        String order = "ASC";
        String columnKey = "id";
        String orderColumns = "summary,issue,assignee";
        String expected = JiraIssueSortableHelper.DOUBLE_QUOTE + columnKey + JiraIssueSortableHelper.DOUBLE_QUOTE + " " + order + ",summary,assignee";
        Assert.assertEquals(expected, JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumns, getJiraColumnInfo()));
    }

    private Map<String, JiraColumnInfo> getJiraColumnInfo()
    {
        Map<String, JiraColumnInfo> jiraColumnInfoMap = Maps.newHashMap();
        jiraColumnInfoMap.put("key", new JiraColumnInfo("key", "Key", Lists.newArrayList("id", "issue", "issuekey")));
        return jiraColumnInfoMap;
    }

}
