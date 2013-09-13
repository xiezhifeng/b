package com.atlassian.confluence.extra.jira.services;

import junit.framework.Assert;

import org.junit.Test;

public class TestJqlBuilder
{
    @Test
    public void buildWithIssue()
    {
        String jqlQuery = new JqlBuilder()
        .put("key", "TP-5", "TP-6")
        .build();
        Assert.assertEquals("jql=key IN(TP-5,TP-6)", jqlQuery);
    }
    
    @Test
    public void buildJqlCombineSingleEqualAndInCriteria()
    {
        String jqlQuery = new JqlBuilder()
        .put("type", "epic")
        .put("status", "open")
        .put("key", "TP-5", "TP-6")
        .build();
        Assert.assertEquals("jql=status=open AND type=epic AND key IN(TP-5,TP-6)", jqlQuery);
    }
    
    @Test
    public void buildIssueKeys()
    {
        String jqlQuery = new JqlBuilder()
        .issueKeys("TP-1", "TP-2")
        .build();
        Assert.assertEquals("jql=key IN(TP-1,TP-2)", jqlQuery);
    }
    
    @Test
    public void buildStatuses()
    {
        String jqlQuery = new JqlBuilder()
        .statuses("open","close")
        .build();
        Assert.assertEquals("jql=status IN(open,close)", jqlQuery);
    }
    
    @Test
    public void buildProjects()
    {
        String jqlQuery = new JqlBuilder()
        .projectKeys("jira-content","JIM")
        .build();
        Assert.assertEquals("jql=project IN(jira-content,JIM)", jqlQuery);
    }
    
    @Test
    public void buildCombineIssueKeyAndStatus()
    {
        String jqlQuery = new JqlBuilder()
        .statuses("open")
        .issueKeys("TP-1")
        .build();
        Assert.assertEquals("jql=status IN(open) AND key IN(TP-1)", jqlQuery);
    }
}
