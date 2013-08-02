package com.atlassian.confluence.plugins.jiracharts;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.applinks.api.*;
import com.atlassian.sal.api.net.Request;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.context.HttpContext;

public class JiraChartMacro implements Macro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy?jql=%s&statType=%s&appId=%s&chartType=pie";
    private static final String TEMPLATE_PATH = "templates/jirachart";
    private ApplicationLinkService applicationLinkService;
    private HttpContext httpContext;
    private ApplicationLinkService appLinkService;
    
    @Override
    public String execute(Map<String, String> parameters, String arg1, ConversionContext arg2) throws MacroExecutionException
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        HttpServletRequest req = httpContext.getRequest();
        String baseUrl = req.getContextPath();

        String serverId = parameters.get("serverId");
        contextMap.put("oAuthUrl", getOauUrl(serverId));

        String url = baseUrl + String.format(SERVLET_PIE_CHART, parameters.get("jql"), parameters.get("statType"), parameters.get("serverId"));
        StringBuffer urlFull = new StringBuffer(url);
        String width = parameters.get("width");
        if(!StringUtils.isBlank(width))
        {
            urlFull.append("&width=" + width + "&height=" + (Integer.parseInt(width) * 2/3));
        }
        contextMap.put("srcImg", urlFull.toString());
        contextMap.put("border", Boolean.parseBoolean(parameters.get("border")));
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/piechart.vm", contextMap);
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
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext context)
    {
        try
        {
            if(parameters.get("jql") != null && parameters.get("statType") != null && parameters.get("width") != null && parameters.get("border") != null && parameters.get("serverId") != null) 
            {
                ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(parameters.get("serverId")));
                if(appLink != null)
                {
                    String url = String.format(SERVLET_PIE_CHART, parameters.get("jql"), parameters.get("statType"), parameters.get("serverId"));
                    return new DefaultImagePlaceholder(url, null, false);
                }
            }
        } catch(TypeNotInstalledException e)
        {
           log.error("error applink", e);
        }
        return null;
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

    public void setApplicationLinkService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
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
