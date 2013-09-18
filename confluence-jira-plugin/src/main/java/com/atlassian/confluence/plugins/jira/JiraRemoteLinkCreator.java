package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.application.jira.JiraManifestProducer;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.util.JiraIssuePredicates;
import com.atlassian.confluence.json.json.Json;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.lang.String;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.sal.api.net.Request.MethodType.POST;

public class JiraRemoteLinkCreator
{
    private final static Logger LOGGER = LoggerFactory.getLogger(JiraRemoteLinkCreator.class);

    
    private final XhtmlContent xhtmlContent;
    private final ApplicationLinkService applicationLinkService;
    private final HostApplication hostApplication;
    private final SettingsManager settingsManager;
    private final JiraMacroFinderService macroFinderService;

    public JiraRemoteLinkCreator(XhtmlContent xhtmlContent, ApplicationLinkService applicationLinkService, 
            HostApplication hostApplication, SettingsManager settingsManager, JiraMacroFinderService finderService)
    {
        this.xhtmlContent = xhtmlContent;
        this.applicationLinkService = applicationLinkService;
        this.hostApplication = hostApplication;
        this.macroFinderService = finderService;
        this.settingsManager = settingsManager;
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

    public void createLinkToIssue(AbstractPage page, String applinkId, String issueKey)
    {
        try
        {
            final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();
            ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(applinkId));
            if (applicationLink != null)
            {
                createRemoteIssueLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey);
            }
            else
            {
                LOGGER.warn("Failed to create a remote link to {} for the application link ID '{}'. Reason: Application link not found.",
                    issueKey,
                    applinkId);
            }
        }
        catch (TypeNotInstalledException e)
        {
            LOGGER.warn("Failed to create a remote link to {} for the application link ID '{}'. Reason: Application link type is currently not installed",
                issueKey,
                applinkId);
        }
    }

    public void createLinkToSprint(AbstractPage page, String applinkId, String sprintId)
    {
        try
        {
            ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(applinkId));
            if (applicationLink != null)
            {
                createRemoteSprintLink(applicationLink, sprintId, page.getIdAsString());
            }
            else
            {
                LOGGER.warn("Failed to create a remote link to the sprint with ID '{}' for the application link ID '{}'. Reason: Application link not found.",
                    sprintId,
                    applinkId);
            }
        }
        catch (TypeNotInstalledException e)
        {
            LOGGER.warn("Failed to create a remote link to the sprint with ID '{}' for the application link ID '{}'. Reason: Application link type is currently not installed",
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

    private void createRemoteSprintLink(final ApplicationLink applicationLink, final String sprintId, final String pageId)
    {
        final Json requestJson = new JsonObject()
            .setProperty("globalId", "appId=" + hostApplication.getId().get() + "&pageId=" + pageId)
            .setProperty("relationship", "linked to");
        final String requestUrl = "rest/greenhopper/1.0/api/sprints/" + GeneralUtil.urlEncode(sprintId) + "/remotelink";
        createRemoteLink(applicationLink, requestJson, requestUrl, sprintId);
    }

    private void createRemoteIssueLink(final ApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String issueKey)
    {
        final Json remoteLink = new JsonObject()
        .setProperty("globalId", "appId=" + hostApplication.getId().get() + "&pageId=" + pageId)
        .setProperty("application", new JsonObject()
            .setProperty("type", "com.atlassian.confluence")
            .setProperty("name", settingsManager.getGlobalSettings().getSiteTitle())
        )
        .setProperty("relationship", "mentioned in")
        .setProperty("object", new JsonObject()
            .setProperty("url", canonicalPageUrl)
            .setProperty("title", "Wiki Page")
        );

        final String requestUrl = "rest/api/latest/issue/" + issueKey + "/remotelink";
        createRemoteLink(applicationLink, remoteLink, requestUrl, issueKey);
    }

    private void createRemoteLink(final ApplicationLink applicationLink, final Json requestBody, final String requestUrl, final String entityId)
    {
        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        try
        {
            final ApplicationLinkRequest request = requestFactory.createRequest(POST, requestUrl);
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
        catch (CredentialsRequiredException e)
        {
            LOGGER.info("Authentication was required, but credentials were not available when creating a JIRA Remote Link", e);
        }
        catch (ResponseException e)
        {
            LOGGER.info("Could not create JIRA Remote Link", e);
        }
    }

    private ApplicationLink findApplicationLink(final MacroDefinition macroDefinition) {
        return Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ApplicationLink>()
        {
            public boolean apply(ApplicationLink input)
            {
                final String serverName = macroDefinition.getParameters().get("server");
                return input.getName().equals(serverName);
            }
        });
    }
}
