package com.atlassian.confluence.extra.jira.model;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestJiraColumnInfo
{

    @Test
    public void testIsColumnUrlWhenNull()
    {
        JiraColumnInfo jiraColumnInfo = new JiraColumnInfo();
        assertFalse(jiraColumnInfo.isUrlColumn());
    }

    @Test
    public void testIsColumnUrlWhenColumnIsUrlType()
    {
        JiraColumnInfo.Schema schema = new JiraColumnInfo.Schema("string", "com.atlassian.jira.plugin.system.customfieldtypes:url", 10100);
        JiraColumnInfo jiraColumnInfo = new JiraColumnInfo("URL", "URL", Lists.newArrayList(), true, schema);
        assertTrue(jiraColumnInfo.isUrlColumn());
    }

    @Test
    public void testIsColumnUrlWhenColumnIsNotUrlType()
    {
        JiraColumnInfo.Schema schema = new JiraColumnInfo.Schema("string", "com.pyxis.greenhopper.jira:gh-epic-label", 10005);
        JiraColumnInfo jiraColumnInfo = new JiraColumnInfo("Epic Name", "Epic Name", Lists.newArrayList(), true, schema);
        assertFalse(jiraColumnInfo.isUrlColumn());
    }

}
