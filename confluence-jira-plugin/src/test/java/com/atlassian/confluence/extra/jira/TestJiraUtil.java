package com.atlassian.confluence.extra.jira;

import java.util.Map;

import com.atlassian.confluence.extra.jira.util.JiraUtil;

import com.google.common.collect.Maps;

import org.junit.Assert;

import junit.framework.TestCase;

public class TestJiraUtil extends TestCase
{

    public void testGetSingleIssueKey()
    {
        Assert.assertNull(JiraUtil.getSingleIssueKey(null, true));
        Assert.assertNull(JiraUtil.getSingleIssueKey(null, false));
        Map<String, String> params = Maps.newHashMap();
        params.put("", "DEMO-1");
        Assert.assertEquals("DEMO-1", JiraUtil.getSingleIssueKey(params, true));
        params.put("", "DEMO-2");
        Assert.assertEquals("DEMO-2", JiraUtil.getSingleIssueKey(params, false));
        params.put("","Not a key");
        Assert.assertNull(JiraUtil.getSingleIssueKey(params, true));
        Assert.assertEquals("Not a key", JiraUtil.getSingleIssueKey(params, false));
    }
}
