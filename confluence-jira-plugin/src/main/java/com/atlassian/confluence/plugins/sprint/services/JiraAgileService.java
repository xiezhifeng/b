package com.atlassian.confluence.plugins.sprint.services;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.sal.api.net.ResponseException;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface JiraAgileService
{
    @Nonnull
    String getBoards(@Nonnull ApplicationLink applicationLink) throws CredentialsRequiredException, ResponseException;

    @Nonnull
    String getSprints(@Nonnull ApplicationLink applicationLink, @Nonnull String boardId) throws CredentialsRequiredException, ResponseException;

    /**
     * Build the KEY IN JQL and send a GET request to JIRA fot the results
     *
     * @param applicationId          ID of the JIRA server
     * @param key               ID of sprint
     * @return a map that contains the resulting element map and the JIRA server URL prefix for a single issue, e.g.:
     * http://jira.example.com/browse/
     */
    JiraSprintModel getJiraSprint(@Nonnull ApplicationLink applicationId, String key) throws CredentialsRequiredException, ResponseException;
}
