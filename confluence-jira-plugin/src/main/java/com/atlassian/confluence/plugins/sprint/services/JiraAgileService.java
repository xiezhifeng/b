package com.atlassian.confluence.plugins.sprint.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.sal.api.net.ResponseException;

import javax.annotation.Nonnull;

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
     * @param sprintId               ID of sprint
     * @return a JiraSprintModel
     */
    JiraSprintModel getJiraSprint(@Nonnull ApplicationLink applicationId, @Nonnull String sprintId) throws CredentialsRequiredException, ResponseException;
}
