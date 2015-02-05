package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;

public class JiraRequestData
{
    private String requestData;
    private Type requestType;
    private boolean isStaticMode;
    
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

    public boolean isStaticMode() {
        return isStaticMode;
    }

    public void setStaticMode(boolean isStaticMode) {
        this.isStaticMode = isStaticMode;
    }
}
