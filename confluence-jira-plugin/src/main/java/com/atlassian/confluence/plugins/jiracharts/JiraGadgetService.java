package com.atlassian.confluence.plugins.jiracharts;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.sal.api.net.Request;

public class JiraGadgetService
{

    private final ApplicationLinkService applicationLinkService;

    public JiraGadgetService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }

    public String requestRestGadget(String appId, String url) throws Exception
    {
        ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(appId));
        ApplicationLinkRequest request = createRequest(applicationLink, Request.MethodType.GET, url);
        return request.execute();
    }

    /**
     * Create request to JIRA, try create request by logged-in user first then
     * anonymous user
     *
     * @param appLink jira server app link
     * @param baseRestUrl (without host) rest endpoint url
     * @return applink's request
     * @throws CredentialsRequiredException
     */
    private ApplicationLinkRequest createRequest(ApplicationLink appLink, Request.MethodType methodType, String baseRestUrl) throws CredentialsRequiredException
    {
        ApplicationLinkRequestFactory requestFactory;
        ApplicationLinkRequest request;

        String url = appLink.getDisplayUrl() + baseRestUrl;

        requestFactory = appLink.createAuthenticatedRequestFactory();
        try
        {
            request = requestFactory.createRequest(methodType, url);
        }
        catch (CredentialsRequiredException e)
        {
            requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            request = requestFactory.createRequest(methodType, url);
        }

        return request;
    }
}
