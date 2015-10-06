package com.atlassian.confluence.plugins.sprint.services;

import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.sal.api.net.ResponseException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Service responsible for getting JIRA Agile data
 */
public interface JiraAgileService
{
    static final String ELEMENT_MAP = "elementMap";
    static final String JIRA_SERVER_URL = "jiraServerUrl";

    /**
     * Build the KEY IN JQL and send a GET request to JIRA fot the results
     *
     * @param serverId          ID of the JIRA server
     * @param key               ID of sprint
     * @return a map that contains the resulting element map and the JIRA server URL prefix for a single issue, e.g.:
     * http://jira.example.com/browse/
     */
    JiraSprintModel getJiraSprint(String serverId, String key) throws MacroExecutionException, CredentialsRequiredException, IOException, ResponseException;
}
