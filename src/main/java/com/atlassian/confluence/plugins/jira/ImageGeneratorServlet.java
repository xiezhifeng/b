package com.atlassian.confluence.plugins.jira;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.plugin.PluginAccessor;

public class ImageGeneratorServlet extends HttpServlet
{
    private static final String IMAGE_JIM_PATH = "jira/jira-issues-count.png";
    private static final String JIRA_CHART_PROXY_SERVLET = "/plugins/servlet/jira-chart-proxy";
    private static final String TEXT_IMAGE_JIRA_CHART = "JIRA Chart | type = pie | jql = %s | statType = %s";
    
    private static final String PLUGIN_KEY = "confluence.extra.jira";
    private static final String SPACE_CHARACTER = " ";
    private static final int FONT_SIZE = 13;
    private static final int ADDED_IMAGE_SIZE = 5;
    private static final int THUMB_JIRA_CHART_WIDTH = 420;
    private static final int THUMB_JIRA_CHART_HEIGHT = 300;
    private static final int PADDING_TOP_CHART = 12;
    private static final int PADDING_TOP_TEXT = 8;

    private I18NBeanFactory i18NBeanFactory;
    private PluginAccessor pluginAccessor;

    private String getText(String key)
    {
        return i18NBeanFactory.getI18NBean().getText(key);
    }

    public void setI18NBeanFactory(I18NBeanFactory i18NBeanFactory)
    {
        this.i18NBeanFactory = i18NBeanFactory;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        BufferedImage bufferedImage = null;
        if("jirachart".equals(req.getParameter("macro")))
        {
            bufferedImage = renderImageJiraChartMacro(req, resp);
        }
        else
        {
            bufferedImage = renderImageJiraIssuesMacro(req, resp);
        }
        
        resp.setContentType("image/png");
        ImageIO.write(bufferedImage, "png", resp.getOutputStream());
    }
    
    private BufferedImage renderImageJiraIssuesMacro(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        String totalIssues = req.getParameter("totalIssues").equals("-1") ? "X" : req.getParameter("totalIssues");
        String totalIssuesText = totalIssues + SPACE_CHARACTER + getText("jiraissues.issues");

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
    
    private BufferedImage renderImageJiraChartMacro(HttpServletRequest req, HttpServletResponse resp) throws IOException 
    {

        String jql = req.getParameter("jql");
        String statType = req.getParameter("statType");
        String appId = req.getParameter("serverId");
        StringBuffer url = new StringBuffer(GeneralUtil.getGlobalSettings().getBaseUrl() + JIRA_CHART_PROXY_SERVLET);
        url.append("?jql=" + URLEncoder.encode(jql, "UTF-8"));
        url.append("&statType=" + statType);
        url.append("&appId=" + appId);
        url.append("&chartType=pie");
        
        BufferedImage chart   = ImageIO.read(new URL(url.toString()));

        int chartWidth  = chart.getWidth();
        int chartHeight = chart.getHeight();
        int chartPadX   = (THUMB_JIRA_CHART_WIDTH - chartWidth)/2;
        int chartPadY   = (THUMB_JIRA_CHART_HEIGHT - chartHeight)/2 + PADDING_TOP_CHART;

        BufferedImage placeholder  = new BufferedImage(THUMB_JIRA_CHART_WIDTH, THUMB_JIRA_CHART_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = drawImage(placeholder, chart, chartPadX, chartPadY, chartWidth, chartHeight);

        //draw icon
        BufferedImage iconBufferImage = getIconBufferImage();
        g.drawImage(iconBufferImage, ADDED_IMAGE_SIZE, 0, iconBufferImage.getWidth(), iconBufferImage.getHeight(), null);
        
        //Text Jira chart macro
        String textImage =  String.format(TEXT_IMAGE_JIRA_CHART, URLDecoder.decode(jql , "UTF-8"), statType);
        g.drawString(textImage, ADDED_IMAGE_SIZE + iconBufferImage.getWidth(), (int)(iconBufferImage.getHeight()/2) + PADDING_TOP_TEXT);

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

    public void setPluginAccessor(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }
}