package com.atlassian.confluence.extra.jira;

import java.util.List;

import com.atlassian.applinks.api.ApplicationLink;

public interface JiraCacheManager
{

    public void clearJiraIssuesCache(final String url, List<String> columns, final ApplicationLink appLink,
            boolean forceAnonymous, boolean isAnonymous);

}