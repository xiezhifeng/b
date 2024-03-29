package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JiraHtmlChartModel;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;

import java.net.SocketTimeoutException;

public abstract class JiraHtmlChart implements JiraChart
{
    protected ReadOnlyApplicationLinkService applicationLinkService;

    protected I18NBeanFactory i18NBeanFactory;

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
            ReadOnlyApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(appId));
            ApplicationLinkRequest request = createRequest(applicationLink, Request.MethodType.GET, url);
            return new Gson().fromJson(request.execute(), getChartModelClass());
        }
        catch (ResponseException e)
        {
            if (e.getCause() instanceof SocketTimeoutException)
            {
                throw new MacroExecutionException(i18NBeanFactory.getI18NBean().getText("jirachart.error.timeout.connection"), e);
            }

            throw new MacroExecutionException(i18NBeanFactory.getI18NBean().getText("jirachart.error.execution"), e);
        }
        catch (Exception e)
        {
            throw new MacroExecutionException(i18NBeanFactory.getI18NBean().getText("jirachart.error.execution"), e);
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
    private ApplicationLinkRequest createRequest(ReadOnlyApplicationLink appLink, Request.MethodType methodType, String baseRestUrl) throws CredentialsRequiredException
    {
        ApplicationLinkRequestFactory requestFactory;
        ApplicationLinkRequest request;

        String url = appLink.getRpcUrl() + baseRestUrl;

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

    @Override
    public boolean isVerifyChartSupported()
    {
        return false;
    }
}
