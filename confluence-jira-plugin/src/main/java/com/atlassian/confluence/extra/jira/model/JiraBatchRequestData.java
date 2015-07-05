package com.atlassian.confluence.extra.jira.model;

import org.jdom.Element;

import java.util.Map;

/**
 * This is a representation of the batch request (per JIRA server) for single issues
 */
public class JiraBatchRequestData
{
    private Map<String, Element> elementMap; // Map of (JIRA Issue Key, JDOM Element) pairs

    private String serverUrl;

    private Exception exception;

    private JiraBatchProcessor jiraBatchProcessor;

    public Map<String, Element> getElementMap()
    {
        return elementMap;
    }

    public void setElementMap(Map<String, Element> elementMap)
    {
        this.elementMap = elementMap;
    }

    public String getServerUrl()
    {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl)
    {
        this.serverUrl = serverUrl;
    }

    public Exception getException()
    {
        return exception;
    }

    public void setException(Exception exception)
    {
        this.exception = exception;
    }

    public JiraBatchProcessor getJiraBatchProcessor()
    {
        return jiraBatchProcessor;
    }

    public void setJiraBatchProcessor(JiraBatchProcessor jiraBatchProcessor)
    {
        this.jiraBatchProcessor = jiraBatchProcessor;
    }
}
