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
        PluginAccessor pa = (PluginAccessor) ContainerManager.getComponent("pluginAccessor");
        InputStream in = pa.getPlugin(PLUGIN_KEY).getClassLoader().getResourceAsStream(IMAGE_PATH);
        String totalIssues = req.getParameter("totalIssues").equals("-1") ? "X" : req.getParameter("totalIssues");
        String totalIssuesText = totalIssues + SPACE_CHARACTER + getText("jiraissues.issues");

        BufferedImage originalImage = ImageIO.read(in);
        Font font = new Font("Arial", Font.PLAIN, 12);
        Graphics2D originalGraphic = originalImage.createGraphics();
        originalGraphic.setFont(font);
        FontMetrics fm = originalGraphic.getFontMetrics(font);

        int bufferedImageSize = originalImage.getWidth() + fm.stringWidth(totalIssuesText) + 10;
        BufferedImage bufferedImage = new BufferedImage(bufferedImageSize, originalImage.getHeight(), originalImage.getType());
        bufferedImage = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.drawImage(originalImage, 0, 0, originalImage.getWidth(), originalImage.getHeight(), null);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(font);
        graphics.setColor(Color.BLACK);
        int textYPosition = (bufferedImage.getHeight() + fm.getAscent()) / 2;
        graphics.drawString(totalIssuesText, originalImage.getWidth() + 5, textYPosition);
        resp.setContentType("image/png");
        ImageIO.write(bufferedImage, "png", resp.getOutputStream());
    }
}