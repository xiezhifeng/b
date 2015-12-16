package com.atlassian.confluence.extra.jira.model;

import org.jdom.Element;

import java.util.Map;

/**
 * This is a representation of the batch request (per JIRA server) for single issues
 */
public class JiraBatchRequestData
{
    private Map<String, Element> elementMap; // Map of (JIRA Issue Key, JDOM Element) pairs

    private String displayUrl;

    private Exception exception;

    public Map<String, Element> getElementMap()
    {
        return elementMap;
    }

    public void setElementMap(Map<String, Element> elementMap)
    {
        this.elementMap = elementMap;
    }

    public String getDisplayUrl()
    {
        return displayUrl;
    }

    public void setDisplayUrl(String displayUrl)
    {
        this.displayUrl = displayUrl;
    }

    public Exception getException()
    {
        return exception;
    }

    public void setException(Exception exception)
    {
        this.exception = exception;
    }
}
