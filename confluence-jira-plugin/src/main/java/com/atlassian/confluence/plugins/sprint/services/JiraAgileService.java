package com.atlassian.confluence.plugins.sprint.services;

import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.sal.api.net.ResponseException;

import javax.annotation.Nonnull;

public interface JiraAgileService
{
    /**
     * Get all scrum boards on one JIRA server
     * @param readOnlyApplicationLink
     * @return a JsonString from JIRA
     * @throws CredentialsRequiredException
     * @throws ResponseException
     */
    @Nonnull
    String getBoards(@Nonnull ReadOnlyApplicationLink readOnlyApplicationLink, String nameFilter) throws CredentialsRequiredException, ResponseException;

    /**
     * Get all sprints on one board
     * @param readOnlyApplicationLink
     * @param boardId
     * @return a JsonString from JIRA
     * @throws CredentialsRequiredException
     * @throws ResponseException
     */
    @Nonnull
    String getSprints(@Nonnull ReadOnlyApplicationLink readOnlyApplicationLink, @Nonnull String boardId) throws CredentialsRequiredException, ResponseException;

    /**
     * Get sprint information
     *
     * @param readOnlyApplicationLink ID of the JIRA server
     * @param sprintId ID of sprint
     * @return a JiraSprintModel
     */
    JiraSprintModel getJiraSprint(@Nonnull ReadOnlyApplicationLink readOnlyApplicationLink, @Nonnull String sprintId) throws CredentialsRequiredException, ResponseException;
}
