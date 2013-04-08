package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;

public interface ProjectKeyCache
{
    public ApplicationLink getAppForKey(String projectKey);
    
    
    
    
    public String getBaseUrlForKey(String projectKey);
}
