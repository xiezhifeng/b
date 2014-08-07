package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;

import java.util.List;

public interface JiraCacheManager
{

    public void clearJiraIssuesCache(final String url, List<String> columns, final ApplicationLink appLink,
            boolean forceAnonymous, boolean isAnonymous);

}