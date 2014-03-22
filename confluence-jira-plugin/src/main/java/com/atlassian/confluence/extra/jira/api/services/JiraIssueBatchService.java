package com.atlassian.confluence.extra.jira.api.services;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.macro.MacroExecutionException;

import java.util.Map;
import java.util.Set;

/**
 * Service responsible for sending batch request to a JIRA server and get the results
 */
public interface JiraIssueBatchService
{

    static final String ELEMENT_MAP = "elementMap";
    static final String JIRA_SERVER_URL = "jiraServerUrl";
    static final Long SUPPORTED_JIRA_SERVER_BUILD_NUMBER = 6097L;

    /**
     * Build the KEY IN JQL and send a GET request to JIRA fot the results
     *
     * @param serverId          ID of the JIRA server
     * @param keys              a set of keys to be put in the KEY IN JQL
     * @param conversionContext the current ConversionContext
     * @return a map that contains the resulting element map and the JIRA server URL prefix for a single issue, e.g.:
     * http://jira.example.com/browse/
     */
    Map<String, Object> getBatchResults(String serverId, Set<String> keys, ConversionContext conversionContext)
            throws MacroExecutionException, UnsupportedJiraServerException;
}
