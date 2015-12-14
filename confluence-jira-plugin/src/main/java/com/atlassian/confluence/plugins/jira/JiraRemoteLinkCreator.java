package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.util.JiraIssuePredicates;
import com.atlassian.confluence.json.json.Json;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.sprint.JiraSprintMacro;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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

    
    private final ReadOnlyApplicationLinkService applicationLinkService;
    private final HostApplication hostApplication;
    private final SettingsManager settingsManager;
    private final JiraMacroFinderService macroFinderService;
    private RequestFactory requestFactory;

    public JiraRemoteLinkCreator(final ReadOnlyApplicationLinkService applicationLinkService, final HostApplication hostApplication, final SettingsManager settingsManager, final JiraMacroFinderService macroFinderService, final RequestFactory requestFactory)
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
            return Sets.newHashSet(macroFinderService.findJiraMacros(page, Predicates.or(JiraIssuePredicates.isSingleIssue, JiraIssuePredicates.isSprintMacro)));
        }
        catch(XhtmlException ex)
        {
            throw new IllegalStateException("Could not parse Create JIRA Issue macros", ex);
        }
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
        ReadOnlyApplicationLink applicationLink = findApplicationLink(applinkId, fallbackUrl,
                "Failed to create a remote link to '" + issueKey + "' for the application '" + applinkId + "'.");
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
        ReadOnlyApplicationLink applicationLink = findApplicationLink(applinkId, fallbackUrl,
                "Failed to create a remote link to sprint '" + sprintId + "' for the application '" + applinkId + "'.");
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

    /**
     * Create remote link to JIRA issue
     * @param page          Confluence page to create the remote link for.
     * @param applinkId     Application ID of the JIRA instance of the remote link.
     * @param issueKey      JIRA issue key to create link for
     * @param fallbackUrl   Display URL of the JIRA instance of the remote link, used as a fallback when no match is found for the application ID.
     * @return true if the remote link was successfully created.
     */
    public boolean createRemoteIssueLink(AbstractPage page, String applinkId, String issueKey, String fallbackUrl)
    {
        Preconditions.checkNotNull(page);
        Preconditions.checkArgument(StringUtils.isNotEmpty(applinkId));
        Preconditions.checkArgument(StringUtils.isNotEmpty(issueKey));
        Preconditions.checkArgument(StringUtils.isNotEmpty(fallbackUrl));

        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();

        ReadOnlyApplicationLink applicationLink = findApplicationLink(applinkId, fallbackUrl,
                "Failed to create a remote link to issue '" + issueKey + "' for the application '" + applinkId + "'.");
        if (applicationLink != null)
        {
            createRemoteIssueLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey);
            return true;
        }
        else
        {
            LOGGER.warn("Failed to create a remote link to the issue with ID '{}' for the application link ID '{}'. Reason: Application link not found.",
                    issueKey,
                    applinkId);
            return false;
        }
    }

    private void createRemoteLinks(AbstractPage page, Iterable<MacroDefinition> macroDefinitions)
    {
        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();

        for (MacroDefinition macroDefinition : macroDefinitions)
        {
            ReadOnlyApplicationLink applicationLink = findApplicationLink(macroDefinition);
            if (applicationLink == null)
            {
                LOGGER.warn("Failed to create a remote link to {} in {}. Reason: Application link not found.",
                        StringUtils.defaultString(macroDefinition.getParameters().get("key"), macroDefinition.getParameters().get("sprintName")),
                        macroDefinition.getParameters().get("server"));
                continue;
            }

            if (StringUtils.equals(macroDefinition.getName(), JiraSprintMacro.JIRASPRINT))
            {
                createEmbeddedSprintLink(applicationLink, page, macroDefinition.getParameter(JiraSprintMacro.MACRO_ID_PARAMETER));
            }
            else
            {
                String keyVal = macroDefinition.getParameters().get("key");
                String defaultParam = macroDefinition.getDefaultParameterValue();
                String issueKey = defaultParam != null ? defaultParam : keyVal;
                createRemoteIssueLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey);
            }
        }
    }

    private boolean createRemoteSprintLink(final ReadOnlyApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String sprintId, final String creationToken)
    {
        final Json requestJson = createJsonData(pageId, canonicalPageUrl, creationToken);
        final String requestUrl = applicationLink.getRpcUrl() + "/rest/greenhopper/1.0/api/sprints/" + GeneralUtil.urlEncode(sprintId) + "/remotelinkchecked";
        Request request = requestFactory.createRequest(PUT, requestUrl);
        return createRemoteLink(applicationLink, requestJson, request, sprintId);
    }

    private boolean createEmbeddedSprintLink(ReadOnlyApplicationLink applicationLink, AbstractPage page, String sprintId)
    {
        final String requestUrl = applicationLink.getRpcUrl() + "/rest/greenhopper/1.0/sprint/"+ GeneralUtil.urlEncode(sprintId) + "/pages" ;
        try
        {
            Request request = applicationLink.createAuthenticatedRequestFactory().createRequest(POST, requestUrl);
            JsonObject requestJson =  new JsonObject()
                    .setProperty("pageId", page.getIdAsString())
                    .setProperty("pageTitle", page.getTitle());
            return createRemoteLink(applicationLink, requestJson, request, sprintId);
        }
        catch (CredentialsRequiredException e)
        {
            LOGGER.debug("Authentication was required, but credentials were not available when creating a JIRA Remote Link", e);
        }
        return false;

    }

    private boolean createRemoteEpicLink(final ReadOnlyApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String issueKey, final String creationToken)
    {
        final Json requestJson = createJsonData(pageId, canonicalPageUrl, creationToken);
        final String requestUrl = applicationLink.getRpcUrl() + "/rest/greenhopper/1.0/api/epics/" + GeneralUtil.urlEncode(issueKey) + "/remotelinkchecked";
        Request request = requestFactory.createRequest(PUT, requestUrl);
        return createRemoteLink(applicationLink, requestJson, request, issueKey);
    }

    private void createRemoteIssueLink(final ReadOnlyApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String issueKey)
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

    private boolean createRemoteLink(final ReadOnlyApplicationLink applicationLink, final Json requestBody, final Request request, final String entityId)
    {
        try
        {
            request.addHeader("Content-Type", "application/json");
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
                            throw new LoggingResponseException();
                        case HttpStatus.SC_FORBIDDEN:
                            LOGGER.warn("Failed to create a remote link to {} in {}. Reason: Forbidden", entityId, applicationLink.getName());
                            throw new LoggingResponseException();
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
                            throw new LoggingResponseException();
                    }
                }
            });
        }
        catch (ResponseException e)
        {
            if (!(e instanceof LoggingResponseException))
            {
                LOGGER.info("Could not create JIRA Remote Link", e);
            }
            return false;
        }

        return true;
    }

    protected ReadOnlyApplicationLink findApplicationLink(final MacroDefinition macroDefinition) {
        return Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ReadOnlyApplicationLink>()
        {
            public boolean apply(ReadOnlyApplicationLink input)
            {
                return StringUtils.equals(input.getName(), macroDefinition.getParameters().get("server"))
                        || StringUtils.equals(input.getId().get(), macroDefinition.getParameters().get("serverId"));
            }
        }, applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class));
    }

    private ReadOnlyApplicationLink findApplicationLink(final String applinkId, final String fallbackUrl, String failureMessage)
    {
        ReadOnlyApplicationLink applicationLink = null;

        applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(applinkId));
        if (applicationLink == null && StringUtils.isNotBlank(fallbackUrl))
        {
            // Application links in OnDemand aren't set up using the host application ID, so we have to fall back to checking the referring URL:
            applicationLink = Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ReadOnlyApplicationLink>()
            {
                public boolean apply(ReadOnlyApplicationLink input)
                {
                    return StringUtils.containsIgnoreCase(fallbackUrl, input.getDisplayUrl().toString());
                }
            });
        }

        return applicationLink;
    }

    /**
     * Indicates that the {@link ResponseHandler} triggering the {@link ResponseException} has already printed a message to the logs.
     */
    private class LoggingResponseException extends ResponseException
    {

    }
}
