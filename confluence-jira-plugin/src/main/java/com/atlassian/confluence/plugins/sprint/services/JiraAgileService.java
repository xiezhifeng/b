package com.atlassian.confluence.plugins.sprint.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.sal.api.net.ResponseException;

import javax.annotation.Nonnull;

public interface JiraAgileService
{
    /**
     * get all scrum boards on one JIRA server
     * @param applicationLink
     * @return a JsonString from JIRA
     * @throws CredentialsRequiredException
     * @throws ResponseException
     */
    @Nonnull
    String getBoards(@Nonnull ApplicationLink applicationLink) throws CredentialsRequiredException, ResponseException;

    /**
     * get all sprints on one board
     * @param applicationLink
     * @param boardId
     * @return a JsonString from JIRA
     * @throws CredentialsRequiredException
     * @throws ResponseException
     */
    @Nonnull
    String getSprints(@Nonnull ApplicationLink applicationLink, @Nonnull String boardId) throws CredentialsRequiredException, ResponseException;

    /**
     * Get sprint information
     *
     * @param applicationId          ID of the JIRA server
     * @param sprintId               ID of sprint
     * @return a JiraSprintModel
     */
    JiraSprintModel getJiraSprint(@Nonnull ApplicationLink applicationId, @Nonnull String sprintId) throws CredentialsRequiredException, ResponseException;
}
