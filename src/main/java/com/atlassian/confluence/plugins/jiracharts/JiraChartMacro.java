package com.atlassian.confluence.plugins.jiracharts;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
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
    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy?jql=%s&statType=%s&appId=%s&chartType=pie";
    private static final String TEMPLATE_PATH = "templates/jirachart";
    private static final String IMAGE_GENERATOR_SERVLET = "/plugins/servlet/image-generator";
    private static final String JIRA_CHART_DEFAULT_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jirachart_images/jirachart_placeholder.png";
    private ApplicationLinkService applicationLinkService;
    
    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        try
        {
            ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(parameters.get("serverId")));
            String url = GeneralUtil.getGlobalSettings().getBaseUrl() + String.format(SERVLET_PIE_CHART, parameters.get("jql"), parameters.get("statType"), appLink.getId().toString());
            
            StringBuffer urlFull = new StringBuffer(url);
            
            String width = parameters.get("width");
            if(!StringUtils.isBlank(width)  && Integer.parseInt(width) > 0)
            {
                urlFull.append("&width=" + width + "&height=" + (Integer.parseInt(width) * 2/3));
            }
            
            contextMap.put("srcImg", urlFull.toString());
            contextMap.put("border", Boolean.parseBoolean(parameters.get("border")));
        }
        catch(Exception e)
        {
            log.error("error render image in content page", e);
            contextMap.put("srcImg", GeneralUtil.getGlobalSettings().getBaseUrl() + JIRA_CHART_DEFAULT_PLACEHOLDER_IMG_PATH);
        }
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
            if(jql != null && statType != null && serverId != null) 
            {
                ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(serverId));
                if(appLink != null)
                {
                    StringBuffer url = new StringBuffer(IMAGE_GENERATOR_SERVLET);
                    url.append("?macro=jirachart");
                    url.append("&jql=" + jql);
                    url.append("&statType=" + statType);
                    url.append("&serverId=" + serverId);
                    return new DefaultImagePlaceholder(url.toString(), null, false);
                }
            }
        }
        catch(Exception e)
        {
           log.error("error get image place holder", e);
        }
        return new DefaultImagePlaceholder(JIRA_CHART_DEFAULT_PLACEHOLDER_IMG_PATH, null, false);
    }

    public void setApplicationLinkService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }
}
