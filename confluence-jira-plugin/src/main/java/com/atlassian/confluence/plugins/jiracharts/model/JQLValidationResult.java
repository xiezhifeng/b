package com.atlassian.confluence.plugins.jiracharts.model;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * Contain the validation result after call search API in JIRA
 * 
 */
public class JQLValidationResult
{
    private List<String> errorMgs;

    private String authUrl;
    
    private String filterUrl;

    private int issueCount;

    private String displayUrl;
    
    public List<String> getErrorMgs()
    {
        return errorMgs;
    }

    public JQLValidationResult()
    {
        setAuthUrl("");
        setErrorMgs(Collections.EMPTY_LIST);
    }

    public void setErrorMgs(List<String> errorMgs)
    {
        if (errorMgs == null)
        {
            errorMgs = Collections.emptyList();
        }
        this.errorMgs = errorMgs;
    }

    public String getAuthUrl()
    {
        return authUrl;
    }

    public void setAuthUrl(String oAuthUrl)
    {
        this.authUrl = oAuthUrl;
    }

    public boolean isValidJQL()
    {
        return getErrorMgs().size() == 0;
    }

    public boolean isOAuthNeeded()
    {
        return !StringUtils.isBlank(getAuthUrl());
    }

    public int getIssueCount()
    {
        return issueCount;
    }

    public void setIssueCount(int issueCount)
    {
        this.issueCount = issueCount;
    }

    public String getFilterUrl()
    {
        return filterUrl;
    }

    public void setFilterUrl(String filterUrl)
    {
        this.filterUrl = filterUrl;
    }

    public String getDisplayUrl()
    {
        return displayUrl;
    }

    public void setDisplayUrl(String displayUrl)
    {
        this.displayUrl = displayUrl;
    }
}
