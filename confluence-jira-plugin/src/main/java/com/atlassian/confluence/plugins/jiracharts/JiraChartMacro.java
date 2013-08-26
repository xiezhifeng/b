package com.atlassian.confluence.plugins.jiracharts;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.renderer.template.TemplateRenderer;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.google.common.collect.Maps;

public class JiraChartMacro implements Macro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy?jql=%s&statType=%s&appId=%s&chartType=pie&authenticated=%s";
    private static final String PLUGIN_KEY = "confluence.extra.jira:dialogsJs";
    private static final String TEMPLATE_PATH = "templates/jirachart";
    private static final String SHOW_INFOR_SOY = "Confluence.Templates.ConfluenceJiraPlugin.showInforInJiraChart.soy";
    private ApplicationLinkService applicationLinkService;
    private TemplateRenderer templateRenderer;
    
    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        
        String url = GeneralUtil.getGlobalSettings().getBaseUrl() + String.format(SERVLET_PIE_CHART, parameters.get("jql"), parameters.get("statType"), parameters.get("serverId"), parameters.get("isAuthenticated"));

        StringBuffer urlFull = new StringBuffer(url);
        
        String width = parameters.get("width");
        if(!StringUtils.isBlank(width)  && Integer.parseInt(width) > 0)
        {
            urlFull.append("&width=" + width + "&height=" + (Integer.parseInt(width) * 2/3));
        }
        
        boolean showInfor = Boolean.parseBoolean(parameters.get("showinfor"));
        String showInforStr = null;
        if(showInfor)
        {
            HashMap<String, Object> soyContext = Maps.newHashMap();
            soyContext.put("urlIssue", "http://jira.pyco.vn");
            soyContext.put("totalIssue", "10 issues");
            soyContext.put("staticType", parameters.get("statType"));
            showInforStr =  renderFromSoy(PLUGIN_KEY, SHOW_INFOR_SOY, soyContext);
        }
        
        contextMap.put("srcImg", urlFull.toString());
        contextMap.put("border", Boolean.parseBoolean(parameters.get("border")));
        contextMap.put("html", showInforStr);
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

    public void setApplicationLinkService(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }
    
    public String renderFromSoy(String pluginKey, String soyTemplate, Map<String, Object> soyContext)
    {
        StringBuilder output = new StringBuilder();
        templateRenderer.renderTo(output, pluginKey, soyTemplate, soyContext);
        return output.toString();
    }

    public void setTemplateRenderer(TemplateRenderer templateRenderer)
    {
        this.templateRenderer = templateRenderer;
    }
}
