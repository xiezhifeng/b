package com.atlassian.confluence.extra.jira.model;

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a representation of the batch response (per JIRA server) (temporary for single issue)
 */
public class JiraResponseData implements Serializable
{
    public enum Status {WORKING, COMPLETED}

    private Status status;
    private final String serverId;
    private final int numOfIssues;
    private final AtomicInteger numOfReceivedIssues;
    private Map<String, List<String>> htmlMacro;

    public JiraResponseData(String serverId, int numOfIssues)
    {
        this.serverId = serverId;
        this.numOfIssues = numOfIssues;

        htmlMacro = Maps.newConcurrentMap();
        status = Status.WORKING;
        numOfReceivedIssues = new AtomicInteger();
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public String getServerId()
    {
        return serverId;
    }

    public Map<String, List<String>> getHtmlMacro()
    {
        return htmlMacro;
    }

    public void add(Map<String, List<String>> htmlMacro)
    {
        this.htmlMacro.putAll(htmlMacro);
        if (numOfReceivedIssues.addAndGet(htmlMacro.size()) == numOfIssues)
        {
            status = Status.COMPLETED;
        }
    }
}
