package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.util.JiraIssuePredicates;
import com.atlassian.confluence.json.json.Json;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.atlassian.sal.api.net.Request.MethodType.POST;
import static com.atlassian.sal.api.net.Request.MethodType.PUT;

public class JiraRemoteLinkCreator
{
    private final static Logger LOGGER = LoggerFactory.getLogger(JiraRemoteLinkCreator.class);

    
    private final ApplicationLinkService applicationLinkService;
    private final HostApplication hostApplication;
    private final SettingsManager settingsManager;
    private final JiraMacroFinderService macroFinderService;
    private RequestFactory requestFactory;

    public JiraRemoteLinkCreator(final ApplicationLinkService applicationLinkService, final HostApplication hostApplication, final SettingsManager settingsManager, final JiraMacroFinderService macroFinderService, final RequestFactory requestFactory)
    {
        this.applicationLinkService = applicationLinkService;
        this.hostApplication = hostApplication;
        this.settingsManager = settingsManager;
        this.macroFinderService = macroFinderService;
        this.requestFactory = requestFactory;
    }

    public void createLinksForEmbeddedMacros(AbstractPage page)
    {
        Set<MacroDefinition> macros = getRemoteLinkMacros(page);
        createRemoteLinks(page, macros);
    }

    public void createLinksForEmbeddedMacros(final AbstractPage prevPage, final AbstractPage page)
    {
        final Set<MacroDefinition> macros = getRemoteLinkMacros(page);
        final Set<MacroDefinition> macrosToCreate;
        if (prevPage == null)
        {
            macrosToCreate = macros;
        }
        else
        {
            final Set<MacroDefinition> prevMacros = getRemoteLinkMacros(prevPage);
            macrosToCreate = Sets.difference(macros, prevMacros);
        }
        createRemoteLinks(page, macrosToCreate);
    }
    
    private Set<MacroDefinition> getRemoteLinkMacros(AbstractPage page)
    {
        try
        {
            return macroFinderService.findJiraIssueMacros(page, JiraIssuePredicates.isSingleIssue);
        }
        catch(XhtmlException ex)
        {
            throw new IllegalStateException("Could not parse Create JIRA Issue macros", ex);
        }
    }

    public void createLinkToEpic(AbstractPage page, String applinkId, String issueKey, String fallbackUrl, String creationToken)
    {
        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();
        ApplicationLink applicationLink = findApplicationLink(applinkId, fallbackUrl,
                "Failed to create a remote link to '" + issueKey + "' for the application '" + applinkId + "'.");
        if (applicationLink != null)
        {
            createRemoteEpicLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey, creationToken);
        }
        else
        {
            LOGGER.warn("Failed to create a remote link to {} for the application link ID '{}'. Reason: Application link not found.",
                issueKey,
                applinkId);
        }
    }

    public void createLinkToSprint(AbstractPage page, String applinkId, String sprintId, String fallbackUrl, final String creationToken)
    {
        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();
        ApplicationLink applicationLink = findApplicationLink(applinkId, fallbackUrl,
                "Failed to create a remote link to sprint '" + sprintId + "' for the application '" + applinkId + "'.");
        if (applicationLink != null)
        {
            createRemoteSprintLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), sprintId, creationToken);
        }
        else
        {
            LOGGER.warn("Failed to create a remote link to the sprint with ID '{}' for the application link ID '{}'. Reason: Application link not found.",
                sprintId,
                applinkId);
        }
    }

    private void createRemoteLinks(AbstractPage page, Iterable<MacroDefinition> macroDefinitions)
    {
        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();

        for (MacroDefinition macroDefinition : macroDefinitions)
        {
            String defaultParam = macroDefinition.getDefaultParameterValue();
            String keyVal = macroDefinition.getParameters().get("key");
            String issueKey = defaultParam != null ? defaultParam : keyVal;
            ApplicationLink applicationLink = findApplicationLink(macroDefinition);

            if (applicationLink != null)
            {
                createRemoteIssueLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey);
            }
            else
            {
                LOGGER.warn("Failed to create a remote link to {} in {}. Reason: Application link not found.",
                    issueKey,
                    macroDefinition.getParameters().get("server"));
            }
        }
    }

    private void createRemoteSprintLink(final ApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String sprintId, final String creationToken)
    {
        final Json requestJson = createJsonData(pageId, canonicalPageUrl, creationToken);
        final String requestUrl = applicationLink.getRpcUrl() + "/rest/greenhopper/1.0/api/sprints/" + GeneralUtil.urlEncode(sprintId) + "/remotelinkchecked";
        Request request = requestFactory.createRequest(PUT, requestUrl);
        createRemoteLink(applicationLink, requestJson, request, sprintId);
    }

    private void createRemoteEpicLink(final ApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String issueKey, final String creationToken)
    {
        final Json requestJson = createJsonData(pageId, canonicalPageUrl, creationToken);
        final String requestUrl = applicationLink.getRpcUrl() + "/rest/greenhopper/1.0/api/epics/" + GeneralUtil.urlEncode(issueKey) + "/remotelinkchecked";
        Request request = requestFactory.createRequest(PUT, requestUrl);
        createRemoteLink(applicationLink, requestJson, request, issueKey);
    }

    private void createRemoteIssueLink(final ApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String issueKey)
    {
        try
        {
            final Json remoteLink = createJsonData(pageId, canonicalPageUrl);
            final String requestUrl = "rest/api/latest/issue/" + issueKey + "/remotelink";
            final ApplicationLinkRequest request = applicationLink.createAuthenticatedRequestFactory().createRequest(POST, requestUrl);
            createRemoteLink(applicationLink, remoteLink, request, issueKey);
        }
        catch (CredentialsRequiredException e)
        {
            LOGGER.info("Authentication was required, but credentials were not available when creating a JIRA Remote Link", e);
        }
    }

    private JsonObject createJsonData(final String pageId, final String canonicalPageUrl, final String creationToken)
    {
        return createJsonData(pageId, canonicalPageUrl).setProperty("creationToken", creationToken);
    }

    private JsonObject createJsonData(final String pageId, final String canonicalPageUrl)
    {
        return new JsonObject()
                .setProperty("globalId", "appId=" + hostApplication.getId().get() + "&pageId=" + pageId)
                .setProperty("application", new JsonObject()
                        .setProperty("type", "com.atlassian.confluence")
                        .setProperty("name", settingsManager.getGlobalSettings().getSiteTitle())
                )
                .setProperty("relationship", "mentioned in")
                .setProperty("object", new JsonObject()
                        .setProperty("url", canonicalPageUrl)
                        .setProperty("title", "Page")
                );
    }

    private void createRemoteLink(final ApplicationLink applicationLink, final Json requestBody, final Request request, final String entityId)
    {
        try
        {
            request.setRequestContentType("application/json");
            request.setRequestBody(requestBody.serialize());
            request.execute(new ResponseHandler<Response>()
            {
                public void handle(Response response) throws ResponseException
                {
                    switch (response.getStatusCode())
                    {
                        case HttpStatus.SC_OK:
                            // success - do nothing
                            break;
                        case HttpStatus.SC_CREATED:
                            // success - do nothing
                            break;
                        case HttpStatus.SC_NOT_FOUND:
                            LOGGER.info("Failed to create a remote link in {}. Reason: Remote links are not supported.", applicationLink.getName());
                            break;
                        case HttpStatus.SC_FORBIDDEN:
                            LOGGER.warn("Failed to create a remote link to {} in {}. Reason: Forbidden", entityId, applicationLink.getName());
                            break;
                        default:
                            LOGGER.warn("Failed to create a remote link to {} in {}. Reason: {} - {}", new String[] {
                                entityId,
                                applicationLink.getName(),
                                Integer.toString(response.getStatusCode()),
                                response.getStatusText()
                            });
                            if (LOGGER.isDebugEnabled())
                            {
                                LOGGER.debug("Response body: {}", response.getResponseBodyAsString());
                            }
                    }
                }
            });
        }
        catch (ResponseException e)
        {
            LOGGER.info("Could not create JIRA Remote Link", e);
        }
    }

    protected ApplicationLink findApplicationLink(final MacroDefinition macroDefinition) {
        return Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ApplicationLink>()
        {
            public boolean apply(ApplicationLink input)
            {
                final String serverName = macroDefinition.getParameters().get("server");
                return input.getName().equals(serverName);
            }
        }, applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class));
    }

    private ApplicationLink findApplicationLink(final String applinkId, final String fallbackUrl, String failureMessage)
    {
        ApplicationLink applicationLink = null;

        try
        {
            applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(applinkId));
            if (applicationLink == null && StringUtils.isNotBlank(fallbackUrl))
            {
                // Application links in OnDemand aren't set up using the host application ID, so we have to fall back to checking the referring URL:
                applicationLink = Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ApplicationLink>()
                {
                    public boolean apply(ApplicationLink input)
                    {
                        return StringUtils.containsIgnoreCase(fallbackUrl, input.getDisplayUrl().toString());
                    }
                });
            }
        }
        catch (TypeNotInstalledException e)
        {
            LOGGER.warn(failureMessage + " Reason: Application link type is currently not installed");
        }

        return applicationLink;
    }
}
