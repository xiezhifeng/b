package com.atlassian.confluence.plugins.sprint.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;

public class DefaultJiraAgileService implements JiraAgileService
{
    private static final Logger LOGGER = Logger.getLogger(DefaultJiraAgileService.class);

    private final JiraIssuesManager jiraIssuesManager;
    private final ApplicationLinkResolver applicationLinkResolver;
    private final JiraConnectorManager jiraConnectorManager;
    private final JiraExceptionHelper jiraExceptionHelper;

    /**
     * Default constructor
     *
     * @param jiraIssuesManager       see {@link JiraIssuesManager}
     * @param applicationLinkResolver see {@link ApplicationLinkResolver}
     * @param jiraConnectorManager    see {@link JiraConnectorManager}
     * @param jiraExceptionHelper     see {@link JiraExceptionHelper}
     */
    public DefaultJiraAgileService(JiraIssuesManager jiraIssuesManager, ApplicationLinkResolver applicationLinkResolver, JiraConnectorManager jiraConnectorManager, JiraExceptionHelper jiraExceptionHelper)
    {
        this.jiraIssuesManager = jiraIssuesManager;
        this.applicationLinkResolver = applicationLinkResolver;
        this.jiraConnectorManager = jiraConnectorManager;
        this.jiraExceptionHelper = jiraExceptionHelper;
    }

    public JiraSprintModel getJiraSprint(String serverId, String key) throws MacroExecutionException, CredentialsRequiredException, IOException, ResponseException {
        ApplicationLink appLink = applicationLinkResolver.getAppLinkForServer("", serverId);
        if (appLink == null)
        {
            throw new MacroExecutionException(jiraExceptionHelper.getText("jiraissues.error.noapplinks"));
        }
        String restUrl = "/rest/agile/1.0/sprint/" + key;
        JsonObject jsonObject = retrieveJQLFromFilter(restUrl, appLink);
        JiraSprintModel jiraSprintModel = new Gson().fromJson(jsonObject.toString(), JiraSprintModel.class);
        generateBoardLink(appLink, jiraSprintModel);
        return jiraSprintModel;
    }

    public JsonObject retrieveJQLFromFilter(String url, ApplicationLink appLink) throws ResponseException, CredentialsRequiredException
    {
        final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
        ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, appLink.getDisplayUrl() + url);
        JsonObject jsonObject = (JsonObject) new JsonParser().parse(request.execute());
        return jsonObject;
    }

    private void generateBoardLink(ApplicationLink applicationLink, JiraSprintModel jiraSprintModel)
    {
        String rapidBoardUrl = applicationLink.getDisplayUrl() + "/secure/RapidBoard.jspa?rapidView=" + jiraSprintModel.getOriginBoardId();
        if (StringUtils.equalsIgnoreCase(jiraSprintModel.getState(), "closed"))
        {
            rapidBoardUrl += "&view=reporting&sprint=" + jiraSprintModel.getId();
        }
        else if (StringUtils.equalsIgnoreCase(jiraSprintModel.getState(), "future"))
        {
            rapidBoardUrl+="&view=planning";
        }
        jiraSprintModel.setBoardUrl(rapidBoardUrl);
    }

}
