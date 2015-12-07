package com.atlassian.confluence.plugins.sprint.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class DefaultJiraAgileService implements JiraAgileService
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AGILE_REST_PATH = "/rest/agile/1.0";
    private static final String AGILE_BOARD_REST_PATH = AGILE_REST_PATH + "/board?type=scrum&name=";
    private static final String AGILE_SPRINT_REST_PATH_TEMPLATE = AGILE_REST_PATH + "/board/%s/sprint";
    private static final String AGILE_SPRINT_INFO_REST_PATH_TEMPLATE = AGILE_REST_PATH + "/sprint/%s";

    private final JiraIssuesManager jiraIssuesManager;
    private final JiraExceptionHelper jiraExceptionHelper;

    public DefaultJiraAgileService(JiraIssuesManager jiraIssuesManager, JiraExceptionHelper jiraExceptionHelper)
    {
        this.jiraIssuesManager = jiraIssuesManager;
        this.jiraExceptionHelper = jiraExceptionHelper;
    }

    @Nonnull
    @Override
    public String getBoards(@Nonnull ReadOnlyApplicationLink readOnlyApplicationLink, String nameFilter) throws CredentialsRequiredException, ResponseException
    {
        String jsonStringResult = retrieveJsonString(readOnlyApplicationLink, AGILE_BOARD_REST_PATH + nameFilter);
        return readValues(jsonStringResult);
    }

    @Nonnull
    @Override
    public String getSprints(@Nonnull ReadOnlyApplicationLink readOnlyApplicationLink, @Nonnull final String boardId) throws CredentialsRequiredException, ResponseException
    {
        String jsonStringResult = retrieveJsonString(readOnlyApplicationLink, String.format(AGILE_SPRINT_REST_PATH_TEMPLATE, boardId));
        return readValues(jsonStringResult);
    }

    @Nonnull
    @Override
    public JiraSprintModel getJiraSprint(@Nonnull ReadOnlyApplicationLink readOnlyApplicationLink, @Nonnull String sprintId) throws CredentialsRequiredException, ResponseException
    {
        String restUrl = String.format(AGILE_SPRINT_INFO_REST_PATH_TEMPLATE, sprintId);
        String jsonData = retrieveJsonString(readOnlyApplicationLink, restUrl);
        JiraSprintModel jiraSprintModel = new Gson().fromJson(jsonData, JiraSprintModel.class);
        return jiraSprintModel;
    }

    private String retrieveJsonString(@Nonnull ReadOnlyApplicationLink applicationLink, String restUrl) throws CredentialsRequiredException, ResponseException
    {
        try
        {
            return jiraIssuesManager.retrieveXMLAsString(restUrl, null, applicationLink, false, false);
        }
        catch (IOException e)
        {
            throw new ResponseException("There is a problem processing the response from JIRA: unrecognisable response:" + e.getMessage(), e);
        }
    }

    private String readValues(String responseString) throws ResponseException
    {
        try
        {
            Map o = OBJECT_MAPPER.reader(Map.class).readValue(responseString);
            StringWriter writer = new StringWriter();
            OBJECT_MAPPER.writeValue(writer, o.get("values"));
            return writer.toString();
        }
        catch (IOException e)
        {
            throw new ResponseException("There is a problem processing the response, no 'values' tag", e);
        }
    }
}
