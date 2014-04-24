package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JiraHtmlChartModel;
import com.atlassian.sal.api.net.Request;
import com.google.gson.Gson;

public abstract class JiraHtmlChart implements JiraChart
{
    protected ApplicationLinkService applicationLinkService;

    public abstract Class<? extends JiraHtmlChartModel> getChartModelClass();

    /**
     * Request to jira and map to chart model
     * @param appId applink id
     * @param url gadget rest url
     * @return chart model
     * @throws MacroExecutionException
     */
    protected Object getChartModel(String appId, String url) throws MacroExecutionException
    {
        try
        {
            ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(appId));
            ApplicationLinkRequest request = createRequest(applicationLink, Request.MethodType.GET, url);
            return new Gson().fromJson(request.execute(), getChartModelClass());
        }
        catch (Exception e)
        {
            throw new MacroExecutionException("Can not render chart", e);
        }

    }

    /**
     * Create request to JIRA, try create request by logged-in user first then
     * anonymous user
     *
     * @param appLink jira server app link
     * @param baseRestUrl rest endpoint url
     * @return applink's request
     * @throws com.atlassian.applinks.api.CredentialsRequiredException
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
