package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;

import java.util.Map;

public class JiraRequestData
{
    private String requestData;
    private Type requestType;
    private boolean staticMode;
    private JiraIssuesType issuesType;
    private Map<String, String> parameters;
    private boolean forceAnonymous;
    private boolean useCache;
    private String url;

    public JiraRequestData(String requestData, Type requestType)
    {
        this.requestType = requestType;
        this.requestData = requestData;
    }

    public JiraRequestData(String requestData, Type requestType, Map<String, String> parameters)
    {
        this(requestData, requestType);
        this.parameters = parameters;
    }

    public JiraRequestData(String requestData, Type requestType, Map<String, String> parameters, JiraIssuesType issuesType)
    {
        this(requestData, requestType, parameters);
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

    public Map<String, String> getParameters()
    {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters)
    {
        this.parameters = parameters;
    }

    public boolean isForceAnonymous()
    {
        return forceAnonymous;
    }

    public void setForceAnonymous(boolean forceAnonymous)
    {
        this.forceAnonymous = forceAnonymous;
    }

    public boolean isUseCache()
    {
        return useCache;
    }

    public void setUseCache(boolean useCache)
    {
        this.useCache = useCache;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
}
