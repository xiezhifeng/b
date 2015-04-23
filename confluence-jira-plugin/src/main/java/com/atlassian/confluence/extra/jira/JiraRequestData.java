package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;

public class JiraRequestData
{
    private String requestData;
    private Type requestType;
    private boolean staticMode;
    private JiraIssuesType issuesType;
    
    public JiraRequestData(String requestData, Type requestType)
    {
        this.requestData = requestData;
        this.requestType = requestType;
    }

    public JiraRequestData(String requestData, Type requestType, JiraIssuesType issuesType)
    {
        this(requestData, requestType);
        this.issuesType = issuesType;
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
        return staticMode;
    }

    public void setStaticMode(boolean isStaticMode) {
        this.staticMode = isStaticMode;
    }

    public JiraIssuesType getIssuesType()
    {
        return issuesType;
    }

    public void setIssuesType(JiraIssuesType issuesType)
    {
        this.issuesType = issuesType;
    }
}
