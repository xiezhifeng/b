package com.atlassian.conflunce.plugins.jiracharts;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.applinks.api.*;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.context.HttpContext;
import com.atlassian.sal.api.net.Request;

public class JiraChartMacro implements Macro, EditorImagePlaceholder
{

    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy?jql=%s&statType=%s&width=%s&border=%s&appId=%s&chartType=pie";
    private static final String TEMPLATE_PATH = "templates/jirachart";
    private HttpContext httpContext;
    private ApplicationLinkService appLinkService;

    @Override
    public String execute(Map<String, String> parameters, String arg1,
            ConversionContext arg2) throws MacroExecutionException
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        HttpServletRequest req = httpContext.getRequest();
        String baseUrl = req.getContextPath();

        String serverId = parameters.get("serverId");
        contextMap.put("oAuthUrl", getOauUrl(serverId));

        String url = baseUrl + String.format(SERVLET_PIE_CHART, parameters.get("jql"), parameters.get("statType"), parameters.get("width"), parameters.get("border"), parameters.get("serverId"));
        contextMap.put("srcImg", url);
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/piechart.vm", contextMap);
    }

    private String getOauUrl(String appLinkId)
    {
        try
        {
            ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(appLinkId));
            appLink.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "");
        }
        catch(CredentialsRequiredException e)
        {
            return e.getAuthorisationURI().toString();
        }
        catch (TypeNotInstalledException e){

        }
        return null;
    }

    @Override
    public BodyType getBodyType()
    {
        // TODO Auto-generated method stub
        return BodyType.NONE;
    }

    @Override
    public OutputType getOutputType()
    {
        // TODO Auto-generated method stub
        return OutputType.BLOCK;
    }

    @Override
    public ImagePlaceholder getImagePlaceholder (
            Map<String, String> parameters, ConversionContext context)
    {
        if(parameters.get("jql") != null)
        {
            String url = String.format(SERVLET_PIE_CHART, parameters.get("jql"), parameters.get("statType"), parameters.get("width"), parameters.get("border"), parameters.get("serverId"));
            return new DefaultImagePlaceholder(url, null, false);
        }
        return null;
    }

    public void setHttpContext(HttpContext httpContext)
    {
        this.httpContext = httpContext;
    }

    public void setAppLinkService(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }
}
