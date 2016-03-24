package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChartFactory;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class ImageGeneratorServlet extends ChartProxyServlet
{
    private static final Logger log = LoggerFactory.getLogger(ImageGeneratorServlet.class); 
    private static final String IMAGE_JIM_PATH = "jira/jira-issues-count.png";

    private static final String PLUGIN_KEY = "confluence.extra.jira";
    private static final int FONT_SIZE = 13;
    private static final int ADDED_IMAGE_SIZE = 5;
    private static final int THUMB_JIRA_CHART_WIDTH = 420;
    private static final int THUMB_JIRA_CHART_HEIGHT = 300;
    private static final int PADDING_TOP_CHART = 12;
    private static final int PADDING_TOP_TEXT = 8;

    private I18NBeanFactory i18NBeanFactory;
    private PluginAccessor pluginAccessor;

    public ImageGeneratorServlet(ReadOnlyApplicationLinkService appLinkService, PluginAccessor pluginAccessor
            , I18NBeanFactory i18NBeanFactory, JiraChartFactory jiraChartFactory)
    {
        super(appLinkService, jiraChartFactory);
        this.pluginAccessor = pluginAccessor;
        this.i18NBeanFactory = i18NBeanFactory;
    }

    private String getText(String key, String totalIssuesText)
    {
        return i18NBeanFactory.getI18NBean().getText(key, new String[]{totalIssuesText});
    }

    private String getText(String key)
    {
        return i18NBeanFactory.getI18NBean().getText(key);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        
        if("jirachart".equals(req.getParameter("macro")))
        {
            try
            {
                doProxy(req, resp, MethodType.GET);
            }
            catch(ServletException e)
            {
                log.error("error render jira chart macro", e);
                throw new IOException();
            }
        }
        else
        {
            RenderedImage bufferedImage = renderImageJiraIssuesMacro(req);
            resp.setContentType("image/png");
            ImageIO.write(bufferedImage, "png", resp.getOutputStream());
        }
    }

    //Fix CONF-34264:JIRA Chart Not Rendered when Inserting to Confluence page and throwing 504 Error
    //Using rpc url to make request to jira server by confluence server
    @Override
    protected URI getApplinkURL(ReadOnlyApplicationLink applicationLink)
    {
        return applicationLink.getRpcUrl();
    }
    
    private BufferedImage renderImageJiraIssuesMacro(HttpServletRequest req) throws IOException
    {
        String totalIssuesText = getTotalIssueText(req.getParameter("totalIssues"));

        BufferedImage atlassianIcon = getIconBufferImage();

        Font font = new Font("Arial", Font.PLAIN, FONT_SIZE);
        Graphics2D originalGraphic = atlassianIcon.createGraphics();
        originalGraphic.setFont(font);
        FontMetrics fm = originalGraphic.getFontMetrics(font);

        int bufferedImageSize = atlassianIcon.getWidth() + fm.stringWidth(totalIssuesText) + ADDED_IMAGE_SIZE;
        BufferedImage bufferedImage = new BufferedImage(bufferedImageSize, atlassianIcon.getHeight(), atlassianIcon.getType());

        Graphics2D graphics = drawImage(bufferedImage, atlassianIcon, 0, 0, atlassianIcon.getWidth(), atlassianIcon.getHeight());

        int textYPosition = (bufferedImage.getHeight() + fm.getAscent()) / 2;
        graphics.drawString(totalIssuesText, atlassianIcon.getWidth(), textYPosition);
        
        return bufferedImage;
    }

    private String getTotalIssueText(String totalIssuesParamValue)
    {
        if(StringUtils.isNumeric(totalIssuesParamValue))
        {
            int totalIssues = Integer.parseInt(totalIssuesParamValue);
            if(totalIssues == 1)
            {
                return getText("jiraissues.static.issue.word", totalIssuesParamValue);
            }
            else
            {
                return getText("jiraissues.static.issues.word", totalIssuesParamValue);
            }
        }
        return getText("jiraissues.static.issues.word", "X");
    }
    
    private BufferedImage renderImageJiraChartMacro(String imgLink) throws IOException
    {
        BufferedImage chart   = ImageIO.read(new URL(imgLink));

        int chartWidth  = chart.getWidth();
        int chartHeight = chart.getHeight();
        int chartPadX   = (THUMB_JIRA_CHART_WIDTH - chartWidth)/2;
        int chartPadY   = (THUMB_JIRA_CHART_HEIGHT - chartHeight)/2 + PADDING_TOP_CHART;

        BufferedImage placeholder  = new BufferedImage(THUMB_JIRA_CHART_WIDTH, THUMB_JIRA_CHART_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = drawImage(placeholder, chart, chartPadX, chartPadY, chartWidth, chartHeight);

        //draw icon
        BufferedImage iconBufferImage = getIconBufferImage();
        int iconWidth = iconBufferImage.getWidth();
        int iconHeight = iconBufferImage.getHeight();

        g.drawImage(iconBufferImage, ADDED_IMAGE_SIZE, 0, iconWidth, iconHeight, null);
        
        //Text Jira chart macro
        g.drawString(getText("jirachart.macro.placeholder.title.name"), ADDED_IMAGE_SIZE + iconWidth, iconHeight/2 + PADDING_TOP_TEXT);

        g.dispose();

        return placeholder;
    }
    
    private Graphics2D drawImage(BufferedImage placeholder, BufferedImage imageChart, int imagePosX, int imagePosY, int chartWidth, int chartHeight)
    {
        Graphics2D g = placeholder.createGraphics();
        Font font = new Font("Arial", Font.PLAIN, FONT_SIZE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(Color.DARK_GRAY);
        g.drawImage(imageChart, imagePosX, imagePosY, chartWidth, chartHeight, null);
        return g;
    }
    
    private BufferedImage getIconBufferImage() throws IOException
    {
        InputStream in = pluginAccessor.getPlugin(PLUGIN_KEY).getClassLoader().getResourceAsStream(IMAGE_JIM_PATH);
        return ImageIO.read(in);
    }

    @Override
    protected void handleResponse(ApplicationLinkRequestFactory requestFactory, HttpServletRequest req, HttpServletResponse resp, ApplicationLinkRequest request, ReadOnlyApplicationLink appLink) throws ResponseException
    {
        String imgLink = getRedirectImgLink(request, req, requestFactory, resp, appLink);
        BufferedImage bufferedImage = null;
        try
        {
            bufferedImage = renderImageJiraChartMacro(imgLink);
            resp.setContentType("image/png");
            ImageIO.write(bufferedImage, "png", resp.getOutputStream());
        }
        catch(IOException e)
        {
            throw new ResponseException();
        }
    }
}