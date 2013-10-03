package com.atlassian.confluence.extra.jira;

import java.util.List;

import com.atlassian.applinks.api.ApplicationLink;

public interface JiraIssuesCacheManager
{

    void clearJiraCache(final String url, List<String> columns, final ApplicationLink appLink,
            boolean forceAnonymous, boolean isAnonymous);

}
