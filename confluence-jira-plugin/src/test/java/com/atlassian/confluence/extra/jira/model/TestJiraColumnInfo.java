package com.atlassian.confluence.extra.jira.model;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestJiraColumnInfo
{

    @Test
    public void testIsNotUrlColumnWhenNull()
    {
        final JiraColumnInfo jiraColumnInfo = new JiraColumnInfo();
        assertFalse("Null custom field was formatted as URL",jiraColumnInfo.isUrlColumn());
    }

    @Test
    public void testIsUrlColumnWhenColumnIsUrlType()
    {
        final JiraColumnInfo.Schema schema = new JiraColumnInfo.Schema("string", "com.atlassian.jira.plugin.system.customfieldtypes:url", 10100);
        final JiraColumnInfo jiraColumnInfo = new JiraColumnInfo("URL", "URL", Lists.newArrayList(), true, schema);
        assertTrue("Valid URL custom field was not formatted as URL",jiraColumnInfo.isUrlColumn());
    }

    @Test
    public void testIsNotUrlColumnWhenColumnIsNotUrlType()
    {
        final JiraColumnInfo.Schema schema = new JiraColumnInfo.Schema("string", "com.pyxis.greenhopper.jira:gh-epic-label", 10005);
        final JiraColumnInfo jiraColumnInfo = new JiraColumnInfo("Epic Name", "Epic Name", Lists.newArrayList(), true, schema);
        assertFalse("Column was Epic Name type but formatted as URL",jiraColumnInfo.isUrlColumn());
    }

}
