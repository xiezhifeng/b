package com.atlassian.confluence.plugins.jiracharts;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;

public class JiraChartMacro implements Macro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy?jql=%s&statType=%s&appId=%s&chartType=pie";
    private ApplicationLinkService applicationLinkService;
    
    @Override
    public String execute(Map<String, String> arg0, String arg1, ConversionContext arg2) throws MacroExecutionException
    {
        // TODO Auto-generated method stub
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
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext context)
    {
        try
        {
            if(parameters.get("jql") != null && parameters.get("statType") != null && parameters.get("serverId") != null) 
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

    public void setApplicationLinkService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }
}
