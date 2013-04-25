package com.atlassian.confluence.plugins.jira;

import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.spring.container.ContainerManager;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageGeneratorServlet extends HttpServlet
{
    private static final String IMAGE_PATH = "jira/jira-issues-count.png";
    private static final String PLUGIN_KEY = "confluence.extra.jira";
    private static final String SPACE_CHARACTER = " ";
    private static final int FONT_SIZE = 12;
    private static final int ADDED_IMAGE_SIZE = 5;

    private I18NBeanFactory i18NBeanFactory;

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
        String totalIssues = req.getParameter("totalIssues").equals("-1") ? "X" : req.getParameter("totalIssues");
        String totalIssuesText = totalIssues + SPACE_CHARACTER + getText("jiraissues.issues");

        PluginAccessor pa = (PluginAccessor) ContainerManager.getComponent("pluginAccessor");
        InputStream in = pa.getPlugin(PLUGIN_KEY).getClassLoader().getResourceAsStream(IMAGE_PATH);
        BufferedImage atlassianIcon = ImageIO.read(in);

        Font font = new Font("Arial", Font.PLAIN, FONT_SIZE);
        Graphics2D originalGraphic = atlassianIcon.createGraphics();
        originalGraphic.setFont(font);
        FontMetrics fm = originalGraphic.getFontMetrics(font);

        int bufferedImageSize = atlassianIcon.getWidth() + fm.stringWidth(totalIssuesText) + ADDED_IMAGE_SIZE;
        BufferedImage bufferedImage = new BufferedImage(bufferedImageSize, atlassianIcon.getHeight(), atlassianIcon.getType());

        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.drawImage(atlassianIcon, 0, 0, atlassianIcon.getWidth(), atlassianIcon.getHeight(), null);

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(font);
        graphics.setColor(Color.BLACK);
        int textYPosition = (bufferedImage.getHeight() + fm.getAscent()) / 2;
        graphics.drawString(totalIssuesText, atlassianIcon.getWidth(), textYPosition);

        resp.setContentType("image/png");
        ImageIO.write(bufferedImage, "png", resp.getOutputStream());
    }
}