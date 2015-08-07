package com.atlassian.confluence.extra.jira.model;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * This is a representation of the batch response (per JIRA server) (temporary for single issue)
 */
public class JiraResponseData
{
    public enum Status {WORKING, COMPLETED}

    private Status status;
    private String serverId;
    private int numOfIssues;
    private Map<String, List<String>> htmlMacro;

    public JiraResponseData(String serverId, int numOfIssues)
    {
        this.serverId = serverId;
        this.numOfIssues = numOfIssues;

        htmlMacro = Maps.newConcurrentMap();
        status = Status.WORKING;
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

    public void setServerId(String serverId)
    {
        this.serverId = serverId;
    }

    public Map<String, List<String>> getHtmlMacro()
    {
        return htmlMacro;
    }

    public void setHtmlMacro(Map<String, List<String>> htmlMacro)
    {
        this.htmlMacro = htmlMacro;
    }

    public void add(Map<String, List<String>> htmlMacro)
    {
        this.htmlMacro.putAll(htmlMacro);
        if (htmlMacro.size() == numOfIssues)
        {
            status = Status.COMPLETED;
        }
    }
}
