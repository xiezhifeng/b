package com.atlassian.confluence.plugins.jiracharts;

import java.util.Map;

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
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.velocity.VelocityUtils;

public class JiraChartMacro implements Macro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy?jql=%s&statType=%s&appId=%s&chartType=pie&authenticated=%s";
    private static final String TEMPLATE_PATH = "templates/jirachart";
    private ApplicationLinkService applicationLinkService;
    
    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        String oauUrl = getOauUrl(parameters.get("serverId"));
        String url = GeneralUtil.getGlobalSettings().getBaseUrl() + String.format(SERVLET_PIE_CHART, parameters.get("jql"), parameters.get("statType"), parameters.get("serverId"), StringUtils.isEmpty(oauUrl));

        StringBuffer urlFull = new StringBuffer(url);
        
        String width = parameters.get("width");
        if(!StringUtils.isBlank(width)  && Integer.parseInt(width) > 0)
        {
            urlFull.append("&width=" + width + "&height=" + (Integer.parseInt(width) * 2/3));
        }

        contextMap.put("oAuthUrl", oauUrl);
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
            String jql = parameters.get("jql");
            String statType = parameters.get("statType");
            String serverId = parameters.get("serverId");
            String authenticated = parameters.get("isAuthenticated");
            if(jql != null && statType != null && serverId != null) 
            {
                ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(serverId));
                if(appLink != null)
                {
                    String url = String.format(SERVLET_PIE_CHART, jql, statType, serverId, authenticated);
                    return new DefaultImagePlaceholder(url, null, false);
                }
            }
        }
        catch(TypeNotInstalledException e)
        {
           log.error("error don't exist applink", e);
        }
        return null;
    }

    private String getOauUrl(String appLinkId)
    {
        try
        {
            ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(appLinkId));
            appLink.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "");
        }
        catch(CredentialsRequiredException e)
        {
            return e.getAuthorisationURI().toString();
        }
        catch (TypeNotInstalledException e){
            log.error("AppLink is not exits", e);
        }
        return null;
    }

    public void setApplicationLinkService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }
}
