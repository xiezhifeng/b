package com.atlassian.confluence.plugins.jiracharts.model;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.atlassian.confluence.macro.MacroExecutionException;

/**
 * Contain the validation result after call search API in JIRA
 * 
 * @author duy.luong
 * 
 */
public class JQLValidationResult
{
    private List<String> errorMgs;

    private String authUrl;
    
    private String filterUrl;

    private int issueCount;
    
    private boolean isCallSuccess;

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
        setCallSuccess(true);
        if (errorMgs == null)
        {
            errorMgs = Collections.EMPTY_LIST;
        }
        this.errorMgs = errorMgs;
    }

    public String getAuthUrl()
    {
        return authUrl;
    }

    public void setAuthUrl(String oAuthUrl)
    {
        setCallSuccess(true);
        this.authUrl = oAuthUrl;
    }

    public boolean isValidJQL()
    {
        return isCallSuccess && getErrorMgs().size() == 0;
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

    public boolean isCallSuccess()
    {
        return isCallSuccess;
    }

    public void setCallSuccess(boolean isCallSuccess)
    {
        this.isCallSuccess = isCallSuccess;
    }

    public String getFilterUrl()
    {
        return filterUrl;
    }

    public void setFilterUrl(String filterUrl)
    {
        this.filterUrl = filterUrl;
    }
}
