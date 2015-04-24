package com.atlassian.confluence.extra.jira.model;

import org.jdom.Element;

import java.util.Map;

public class IssueData
{
    private Map<String, String> params;
    private String renderId;
    private String key;
    private Throwable error;
    private Element data;
    private String serverUrl;

    public Map<String, String> getParams()
    {
        return params;
    }

    public void setParams(Map<String, String> params)
    {
        this.params = params;
    }

    public String getRenderId()
    {
        return renderId;
    }

    public void setRenderId(String renderId)
    {
        this.renderId = renderId;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public Throwable getError()
    {
        return error;
    }

    public void setError(Throwable error)
    {
        this.error = error;
    }

    public Element getData()
    {
        return data;
    }

    public void setData(Element data)
    {
        this.data = data;
    }

    public String getServerUrl()
    {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl)
    {
        this.serverUrl = serverUrl;
    }
}
