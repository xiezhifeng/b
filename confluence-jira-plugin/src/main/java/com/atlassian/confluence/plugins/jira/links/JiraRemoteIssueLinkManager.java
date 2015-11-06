package com.atlassian.confluence.plugins.jira.links;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.util.JiraIssuePredicates;
import com.atlassian.confluence.json.json.Json;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.net.RequestFactory;
import com.google.common.collect.Sets;

import java.util.Set;

import static com.atlassian.sal.api.net.Request.MethodType.DELETE;
import static com.atlassian.sal.api.net.Request.MethodType.POST;

public class JiraRemoteIssueLinkManager extends JiraRemoteLinkManager
{

    public JiraRemoteIssueLinkManager(
            ApplicationLinkService applicationLinkService,
            HostApplication hostApplication,
            SettingsManager settingsManager,
            JiraMacroFinderService macroFinderService,
            RequestFactory requestFactory)
    {
        super(applicationLinkService, hostApplication, settingsManager, macroFinderService, requestFactory);
    }

    public void updateIssueLinksForEmbeddedMacros(final AbstractPage prevPage, final AbstractPage page)
    {
        final Set<MacroDefinition> macros = getRemoteLinkMacros(page);
        final Set<MacroDefinition> prevMacros = getRemoteLinkMacros(prevPage);

        updateRemoteLinks(page, Sets.difference(prevMacros, macros), OperationType.DELETE);
        updateRemoteLinks(page, Sets.difference(macros, prevMacros), OperationType.CREATE);
    }

    public void createIssueLinksForEmbeddedMacros(final AbstractPage page)
    {
        final Set<MacroDefinition> macros = getRemoteLinkMacros(page);
        updateRemoteLinks(page, macros, OperationType.CREATE);
    }

    public void deleteIssueLinksForEmbeddedMacros(final AbstractPage page)
    {
        final Set<MacroDefinition> macros = getRemoteLinkMacros(page);
        updateRemoteLinks(page, macros, OperationType.DELETE);
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

    private void updateRemoteLinks(AbstractPage page, Iterable<MacroDefinition> macroDefinitions, OperationType operationType)
    {
        final String baseUrl = GeneralUtil.getGlobalSettings().getBaseUrl();

        for (MacroDefinition macroDefinition : macroDefinitions)
        {
            String defaultParam = macroDefinition.getDefaultParameterValue();
            String keyVal = macroDefinition.getParameters().get("key");
            String issueKey = defaultParam != null ? defaultParam : keyVal;
            ApplicationLink applicationLink = findApplicationLink(macroDefinition);

            if (applicationLink == null)
            {
                LOGGER.warn("Failed to update a remote link to {} in {}. Reason: Application link not found.",
                        issueKey,
                        macroDefinition.getParameters().get("server"));
                continue;
            }

            if (operationType == OperationType.CREATE)
            {
                createRemoteIssueLink(applicationLink, baseUrl + GeneralUtil.getIdBasedPageUrl(page), page.getIdAsString(), issueKey);
            }
            else
            {
                deleteRemoteIssueLink(applicationLink, page.getIdAsString(), issueKey);
            }
        }
    }

    private void createRemoteIssueLink(final ApplicationLink applicationLink, final String canonicalPageUrl, final String pageId, final String issueKey)
    {
        try
        {
            final Json remoteLink = createJsonData(pageId, canonicalPageUrl);
            final String requestUrl = "rest/api/latest/issue/" + issueKey + "/remotelink";
            final ApplicationLinkRequest request = applicationLink.createAuthenticatedRequestFactory().createRequest(POST, requestUrl);
            executeRemoteLinkRequest(applicationLink, remoteLink, request, issueKey, OperationType.CREATE);
        }
        catch (CredentialsRequiredException e)
        {
            LOGGER.info("Authentication was required, but credentials were not available when creating a JIRA Remote Link", e);
        }
    }

    private void deleteRemoteIssueLink(final ApplicationLink applicationLink, final String pageId, final String issueKey)
    {
        try
        {
            final String globalId = getGlobalId(pageId);
            final String requestUrl = "rest/api/latest/issue/" + issueKey + "/remotelink?globalId=" + GeneralUtil.urlEncode(globalId);

            final ApplicationLinkRequest request = applicationLink.createAuthenticatedRequestFactory().createRequest(DELETE, requestUrl);

            executeRemoteLinkRequest(applicationLink, null, request, issueKey, OperationType.DELETE);
        }
        catch (CredentialsRequiredException e)
        {
            LOGGER.info("Authentication was required, but credentials were not available when creating a JIRA Remote Link", e);
        }
    }
}
