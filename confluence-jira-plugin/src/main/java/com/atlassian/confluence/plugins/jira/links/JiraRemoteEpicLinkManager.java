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

public class JiraRemoteEpicLinkManager extends JiraRemoteLinkManager
{
    private final static Logger LOGGER = LoggerFactory.getLogger(JiraRemoteEpicLinkManager.class);

    public JiraRemoteEpicLinkManager(
            ReadOnlyApplicationLinkService applicationLinkService,
            HostApplication hostApplication,
            SettingsManager settingsManager,
            JiraMacroFinderService macroFinderService,
            RequestFactory requestFactory)
    {
        super(applicationLinkService, hostApplication, settingsManager, macroFinderService, requestFactory);
    }

    /**
     * Sends a request to JIRA Agile to create a remote link between the specified Confluence page and JIRA Agile epic.
     *
     * @param page              Confluence page to create the remote link for.
     * @param applinkId         Application ID of the JIRA instance of the remote link.
     * @param issueKey          JIRA issue key to create the remote link for.
     * @param fallbackUrl       Display URL of the JIRA instance of the remote link, used as a fallback when no match is found for the application ID.
     * @param creationToken     One-time token from JIRA Agile to be used in the request.
     * @return true if the remote link was successfully created.
     */
    public boolean createLinkToEpic(AbstractPage page, String applinkId, String issueKey, String fallbackUrl, String creationToken)
    {
        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();
        ReadOnlyApplicationLink applicationLink = findApplicationLink(applinkId, fallbackUrl);
        if (applicationLink != null)
        {
            return createRemoteEpicLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey, creationToken);
        }
        else
        {
            LOGGER.warn("Failed to create a remote link to {} for the application link ID '{}'. Reason: Application link not found.",
                    issueKey,
                    applinkId);
            return false;
        }
    }

    private boolean createRemoteEpicLink(final ReadOnlyApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String issueKey, final String creationToken)
    {
        final Json requestJson = createJsonData(pageId, canonicalPageUrl, creationToken);
        final String requestUrl = applicationLink.getRpcUrl() + "/rest/greenhopper/1.0/api/epics/" + GeneralUtil.urlEncode(issueKey) + "/remotelinkchecked";
        Request request = requestFactory.createRequest(PUT, requestUrl);
        return executeRemoteLinkRequest(applicationLink, requestJson, request, issueKey, OperationType.CREATE);
    }
}
