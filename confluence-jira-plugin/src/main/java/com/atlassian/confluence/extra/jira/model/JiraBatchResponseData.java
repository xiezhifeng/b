package com.atlassian.confluence.extra.jira.model;

import org.jdom.Element;

import java.util.List;
import java.util.Map;

/**
 * This is a representation of the batch reponse (per JIRA server) (temporary for single issue)
 */
public class JiraBatchResponseData
{
    public enum BatchStatus {WORKING, COMPLETED}

    private BatchStatus batchStatus;

    private String serverId;

    public JiraBatchResponseData()
    {
        batchStatus = BatchStatus.WORKING;
    }

    public JiraBatchResponseData(Map<String, List<String>> htmlMacro)
    {
        this.batchStatus = BatchStatus.COMPLETED;
        this.htmlMacro = htmlMacro;
    }

    private Map<String, List<String>> htmlMacro;

    public BatchStatus getBatchStatus()
    {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus)
    {
        this.batchStatus = batchStatus;
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
}
