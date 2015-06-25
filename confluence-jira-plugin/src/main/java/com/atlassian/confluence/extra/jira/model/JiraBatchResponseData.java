package com.atlassian.confluence.extra.jira.model;

import org.jdom.Element;

import java.util.List;
import java.util.Map;

/**
 * This is a representation of the batch reponse (per JIRA server) (temporary for single issue)
 */
public class JiraBatchResponseData
{
    private String serverId;

    private List<String> issueKeys;

    private Map<String, String> htmlMacro;


    public String getServerId()
    {
        return serverId;
    }

    public void setServerId(String serverId)
    {
        this.serverId = serverId;
    }

    public List<String> getIssueKeys()
    {
        return issueKeys;
    }

    public void setIssueKeys(List<String> issueKeys)
    {
        this.issueKeys = issueKeys;
    }

    public Map<String, String> getHtmlMacro()
    {
        return htmlMacro;
    }

    public void setHtmlMacro(Map<String, String> htmlMacro)
    {
        this.htmlMacro = htmlMacro;
    }
}
