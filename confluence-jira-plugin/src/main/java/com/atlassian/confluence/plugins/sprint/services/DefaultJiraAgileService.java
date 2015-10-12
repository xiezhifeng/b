package com.atlassian.confluence.plugins.sprint.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class DefaultJiraAgileService implements JiraAgileService
{
    private static final Logger log = LoggerFactory.getLogger(DefaultJiraAgileService.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AGILE_REST_PATH = "/rest/agile/1.0";
    private static final String AGILE_BOARD_REST_PATH = AGILE_REST_PATH + "/board?type=scrum";
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
    public String getBoards(@Nonnull ApplicationLink applicationLink) throws CredentialsRequiredException, ResponseException
    {
        String jsonStringResult = retrieveJsonString(applicationLink, AGILE_BOARD_REST_PATH);
        return readValues(jsonStringResult);
    }

    @Nonnull
    @Override
    public String getSprints(@Nonnull ApplicationLink applicationLink, @Nonnull final String boardId) throws CredentialsRequiredException, ResponseException
    {
        String jsonStringResult = retrieveJsonString(applicationLink, String.format(AGILE_SPRINT_REST_PATH_TEMPLATE, boardId));
        return readValues(jsonStringResult);
    }

    @Nonnull
    @Override
    public JiraSprintModel getJiraSprint(@Nonnull ApplicationLink applicationLink, String key) throws CredentialsRequiredException, ResponseException
    {
        String restUrl = String.format(AGILE_SPRINT_INFO_REST_PATH_TEMPLATE, key);
        String jsonData = retrieveJsonString(applicationLink, restUrl);
        JiraSprintModel jiraSprintModel = new Gson().fromJson(jsonData, JiraSprintModel.class);
        generateBoardLink(applicationLink, jiraSprintModel);
        return jiraSprintModel;
    }

    private void generateBoardLink(ApplicationLink applicationLink, JiraSprintModel jiraSprintModel)
    {
        String rapidBoardUrl = applicationLink.getDisplayUrl() + "/secure/RapidBoard.jspa?rapidView=" + jiraSprintModel.getOriginBoardId();
        if (StringUtils.equalsIgnoreCase(jiraSprintModel.getState(), "closed"))
        {
            rapidBoardUrl += "&view=reporting&chart=burndownChart&sprint=" + jiraSprintModel.getId();
        }
        else if (StringUtils.equalsIgnoreCase(jiraSprintModel.getState(), "future"))
        {
            rapidBoardUrl+="&view=planning";
        }
        jiraSprintModel.setBoardUrl(rapidBoardUrl);
    }

    private String retrieveJsonString(@Nonnull ApplicationLink applicationId, String restUrl) throws CredentialsRequiredException, ResponseException
    {
        try
        {
            return jiraIssuesManager.retrieveXMLAsString(restUrl, null, applicationId, false, false);
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
