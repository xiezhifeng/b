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
    public void buildWithExtraConfiguration()
    {
        String jqlQuery = new JqlBuilder()
        .put("type", "epic")
        .setStartAt(1)
        .setMaxResults(5)
        .setValidateQuery(true)
        .setFields("summary,key")
        .build();
        Assert.assertEquals("jql=type=epic&startAt=1&maxResults=5&validateQuery=true&fields=summary,key", jqlQuery);
    }
    
}
