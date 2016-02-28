package com.atlassian.confluence.plugins.conluenceview.services;

import java.util.List;

import com.atlassian.confluence.plugins.conluenceview.rest.dto.LinkedSpaceDto;

public interface ConfluenceJiraLinksService
{
    /**
     * @return application link id of OD instance
     */
    String getODApplicationLinkId();

    /**
     * @param jiraUrl jira baseUrl
     * @param projectKey project key
     * @return list of Confluence space that linked into a JIRA project
     */
    List<LinkedSpaceDto> getLinkedSpaces(String jiraUrl, String projectKey);
}
