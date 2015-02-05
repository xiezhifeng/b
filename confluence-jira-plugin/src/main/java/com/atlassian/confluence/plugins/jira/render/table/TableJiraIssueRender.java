package com.atlassian.confluence.plugins.jira.render.table;

import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRender;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import java.util.Map;

public abstract class TableJiraIssueRender extends JiraIssueRender {


    private static final String JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jira-table.png";

    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    @Override
    public ImagePlaceholder getImagePlaceholder(JiraRequestData jiraRequestData, Map<String, String> parameters, String resourcePath)
    {
        return new DefaultImagePlaceholder(JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH, null, false);
    }

    @Override
    public String getMobileTemplate(Map<String, Object> contextMap) {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_MOBILE_PATH + "/mobileJiraIssues.vm", contextMap);
    }
}
