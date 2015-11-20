package com.atlassian.confluence.plugins.jira.links;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.json.json.Json;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.sal.api.net.Request.MethodType.PUT;

public class JiraRemoteSprintLinkManager extends JiraRemoteLinkManager
{
    private final static Logger LOGGER = LoggerFactory.getLogger(JiraRemoteSprintLinkManager.class);

    public JiraRemoteSprintLinkManager(
            ReadOnlyApplicationLinkService applicationLinkService,
            HostApplication hostApplication,
            SettingsManager settingsManager,
            JiraMacroFinderService macroFinderService,
            RequestFactory requestFactory)
    {
        super(applicationLinkService, hostApplication, settingsManager, macroFinderService, requestFactory);
    }

    /**
     * Sends a request to JIRA Agile to create a remote link between the specified Confluence page and JIRA Agile sprint.
     *
     * @param page              Confluence page to create the remote link for.
     * @param applinkId         Application ID of the JIRA instance of the remote link.
     * @param sprintId          Sprint ID to create the remote link for.
     * @param fallbackUrl       Display URL of the JIRA instance of the remote link, used as a fallback when no match is found for the application ID.
     * @param creationToken     One-time token from JIRA Agile to be used in the request.
     * @return true if the remote link was successfully created.
     */
    public boolean createLinkToSprint(AbstractPage page, String applinkId, String sprintId, String fallbackUrl, final String creationToken)
    {
        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();
        ReadOnlyApplicationLink applicationLink = findApplicationLink(applinkId, fallbackUrl);
        if (applicationLink != null)
        {
            return createRemoteSprintLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), sprintId, creationToken);
        }
        else
        {
            LOGGER.warn("Failed to create a remote link to the sprint with ID '{}' for the application link ID '{}'. Reason: Application link not found.",
                    sprintId,
                    applinkId);
            return false;
        }
    }

    private boolean createRemoteSprintLink(final ReadOnlyApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String sprintId, final String creationToken)
    {
        final Json requestJson = createJsonData(pageId, canonicalPageUrl, creationToken);
        final String requestUrl = applicationLink.getRpcUrl() + "/rest/greenhopper/1.0/api/sprints/" + GeneralUtil.urlEncode(sprintId) + "/remotelinkchecked";
        Request request = requestFactory.createRequest(PUT, requestUrl);
        return executeRemoteLinkRequest(applicationLink, requestJson, request, sprintId, OperationType.CREATE);
    }
}
