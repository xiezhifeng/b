package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;

import java.util.Map;

public class JiraRequestData
{
    private String requestData;
    private Type requestType;
    private Map<String, String> parameters;
    
    public JiraRequestData(String requestData, Type requestType)
    {
        this.requestData = requestData;
        this.requestType = requestType;
    }


    public String getRequestData()
    {
        return requestData;
    }


    public void setRequestData(String requestData)
    {
        this.requestData = requestData;
    }


    public Type getRequestType()
    {
        return requestType;
    }


    public void setRequestType(Type requestType)
    {
        this.requestType = requestType;
    }
    
    
}
