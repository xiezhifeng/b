package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
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

    private final static Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\s*([A-Z][A-Z]+)-[0-9]+\\s*");

    private final XhtmlContent xhtmlContent;
    private final ApplicationLinkService applicationLinkService;
    private final HostApplication hostApplication;
    private final SettingsManager settingsManager;

    public JiraRemoteLinkCreator(XhtmlContent xhtmlContent, ApplicationLinkService applicationLinkService, HostApplication hostApplication, SettingsManager settingsManager)
    {
        this.xhtmlContent = xhtmlContent;
        this.applicationLinkService = applicationLinkService;
        this.hostApplication = hostApplication;
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

    public void createLinkToIssue(AbstractPage page, String remoteInstanceUrl, String issueKey)
    {
        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();
        ApplicationLink applicationLink = findApplicationLink(remoteInstanceUrl);
        if (applicationLink != null)
        {
            createRemoteLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey);
        }
        else
        {
            LOGGER.warn("Failed to create a remote link to {} for the application link ID '{}'. Reason: Application link not found.",
                issueKey,
                remoteInstanceUrl);
        }
    }

    private Set<MacroDefinition> getRemoteLinkMacros(AbstractPage page)
    {
        final ImmutableSet.Builder<MacroDefinition> setBuilder = ImmutableSet.builder();
        final ConversionContext conversionContext = new DefaultConversionContext(page.toPageContext());
        try
        {
            xhtmlContent.handleMacroDefinitions(page.getBodyAsString(), conversionContext, new MacroDefinitionHandler()
            {
                public void handle(MacroDefinition macroDefinition)
                {
                    if ("jira".equals(macroDefinition.getName()) && isSingleIssue(macroDefinition))
                    {
                        setBuilder.add(macroDefinition);
                    }
                }
            });
        }
        catch (XhtmlException e)
        {
            throw new IllegalStateException("Could not parse Create JIRA Issue macros", e);
        }

        return setBuilder.build();
    }

    private boolean isSingleIssue(MacroDefinition macroDefinition)
    {
        String defaultParam = macroDefinition.getDefaultParameterValue();
        Map<String, String> parameters = macroDefinition.getParameters();
        return (defaultParam != null && ISSUE_KEY_PATTERN.matcher(defaultParam).matches()) ||
               (parameters != null && parameters.get("key") != null);
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
                createRemoteLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey);
            }
            else
            {
                LOGGER.warn("Failed to create a remote link to {} in {}. Reason: Application link not found.",
                    issueKey,
                    macroDefinition.getParameters().get("server"));
            }
        }
    }

    private void createRemoteLink(final ApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String issueKey)
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

        final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
        try
        {
            final String requestUrl = "rest/api/latest/issue/" + issueKey + "/remotelink";
            final ApplicationLinkRequest request = requestFactory.createRequest(POST, requestUrl);
            request.setRequestContentType("application/json");
            request.setRequestBody(remoteLink.serialize());
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
                            LOGGER.warn("Failed to create a remote link to {} in {}. Reason: Forbidden", issueKey, applicationLink.getName());
                            break;
                        default:
                            LOGGER.warn("Failed to create a remote link to {} in {}. Reason: {} - {}", new String[] {
                                issueKey,
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
        }, null);
    }

    private ApplicationLink findApplicationLink(final String remoteInstanceUrl) {
        return Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ApplicationLink>()
        {
            public boolean apply(ApplicationLink input)
            {
                return input.getDisplayUrl().toString().equals(remoteInstanceUrl);
            }
        }, null);
    }
}
