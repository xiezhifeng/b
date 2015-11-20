package com.atlassian.confluence.plugins.jira.links;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.json.json.Json;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JiraRemoteLinkManager
{
    private final static Logger LOGGER = LoggerFactory.getLogger(JiraRemoteLinkManager.class);
    
    private final ReadOnlyApplicationLinkService applicationLinkService;
    private final HostApplication hostApplication;
    private final SettingsManager settingsManager;
    protected final JiraMacroFinderService macroFinderService;
    protected RequestFactory requestFactory;

    public JiraRemoteLinkManager(
            final ReadOnlyApplicationLinkService applicationLinkService,
            final HostApplication hostApplication,
            final SettingsManager settingsManager,
            final JiraMacroFinderService macroFinderService,
            final RequestFactory requestFactory)
    {
        this.applicationLinkService = applicationLinkService;
        this.hostApplication = hostApplication;
        this.settingsManager = settingsManager;
        this.macroFinderService = macroFinderService;
        this.requestFactory = requestFactory;
    }

    protected enum OperationType
    {
        CREATE, DELETE
    }

    protected JsonObject createJsonData(final String pageId, final String canonicalPageUrl, final String creationToken)
    {
        return createJsonData(pageId, canonicalPageUrl).setProperty("creationToken", creationToken);
    }

    protected JsonObject createJsonData(final String pageId, final String canonicalPageUrl)
    {
        return new JsonObject()
                .setProperty("globalId", getGlobalId(pageId))
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

    protected String getGlobalId(final String pageId)
    {
        return "appId=" + hostApplication.getId().get() + "&pageId=" + pageId;
    }

    protected boolean executeRemoteLinkRequest(final ReadOnlyApplicationLink applicationLink, final Json requestBody, final Request request, final String entityId, final OperationType operationType)
    {
        final String operation = operationType.equals(OperationType.CREATE) ? "create" : "delete";

        try
        {
            request.addHeader("Content-Type", "application/json");

            if (requestBody != null)
            {
                request.setRequestBody(requestBody.serialize());
            }

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
                            LOGGER.info("Failed to {} a remote link in {}. Reason: Remote links are not supported.", operation, applicationLink.getName());
                            throw new LoggingResponseException();
                        case HttpStatus.SC_FORBIDDEN:
                            LOGGER.warn("Failed to {} a remote link to {} in {}. Reason: Forbidden", operation, entityId, applicationLink.getName());
                            throw new LoggingResponseException();
                        default:
                            LOGGER.warn("Failed to {} a remote link to {} in {}. Reason: {} - {}", new String[] {
                                operation,
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
        catch (LoggingResponseException e)
        {

        }
        catch (ResponseException e)
        {
            LOGGER.info("Could not {} JIRA Remote Link", operation, e);
            return false;
        }

        return true;
    }

    protected ReadOnlyApplicationLink findApplicationLink(final MacroDefinition macroDefinition)
    {
        return Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ReadOnlyApplicationLink>()
        {
            public boolean apply(ReadOnlyApplicationLink input)
            {
                final String serverName = macroDefinition.getParameters().get("server");
                return input.getName().equals(serverName);
            }
        }, applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class));
    }

    protected ReadOnlyApplicationLink findApplicationLink(final String applinkId, final String fallbackUrl)
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
