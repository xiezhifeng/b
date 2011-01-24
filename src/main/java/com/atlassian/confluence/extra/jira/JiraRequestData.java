package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;

public class JiraRequestData
{
    String requestData;
    Type requestType;
    
    
    public JiraRequestData(String requestData, Type requestType)
    {
        super();
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
