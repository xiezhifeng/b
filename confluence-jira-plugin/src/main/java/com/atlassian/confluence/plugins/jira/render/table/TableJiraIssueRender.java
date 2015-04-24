package com.atlassian.confluence.plugins.jira.render.table;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssueSortingManager;
import com.atlassian.confluence.extra.jira.JiraIssuesColumnManager;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRender;
import com.atlassian.confluence.util.GeneralUtil;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

public abstract class TableJiraIssueRender extends JiraIssueRender
{
    private static final String JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jira-table.png";
    private static final String DEFAULT_DATA_WIDTH = "100%";

    protected JiraIssueSortingManager jiraIssueSortingManager;
    protected JiraIssuesColumnManager jiraIssuesColumnManager;

    @Override
    public ImagePlaceholder getImagePlaceholder(JiraRequestData jiraRequestData, Map<String, String> parameters, String resourcePath) {
        return new DefaultImagePlaceholder(JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH, null, false);
    }

    @Override
    public void populateSpecifyMacroType(Map<String, Object> contextMap, ApplicationLink appLink, ConversionContext conversionContext, JiraRequestData jiraRequestData) throws MacroExecutionException
    {
        Map<String, String> params = jiraRequestData.getParameters();
        Map<String, JiraColumnInfo> jiraColumns = jiraIssuesColumnManager.getColumnsInfoFromJira(appLink);
        jiraRequestData.setRequestData(jiraIssueSortingManager.getRequestDataForSorting(params, jiraRequestData, jiraColumns, conversionContext, appLink));

        List<JiraColumnInfo> columns = jiraIssuesColumnManager.getColumnInfo(params, jiraColumns, appLink);
        contextMap.put(JiraIssuesMacro.COLUMNS, columns);

        String width = params.get(JiraIssuesMacro.WIDTH);
        if (width == null)
        {
            width = DEFAULT_DATA_WIDTH;
        }
        else if(!width.contains("%") && !width.contains("px"))
        {
            width += "px";
        }
        contextMap.put(JiraIssuesMacro.WIDTH, width);

        String heightStr = JiraUtil.getParamValue(params, JiraIssuesMacro.HEIGHT, JiraUtil.PARAM_POSITION_6);
        if (!StringUtils.isEmpty(heightStr) && StringUtils.isNumeric(heightStr))
        {
            contextMap.put(JiraIssuesMacro.HEIGHT, heightStr);
        }

        //Only define the Title param if explicitly defined.
        if (params.containsKey(JiraIssuesMacro.TITLE))
        {
            contextMap.put(JiraIssuesMacro.TITLE, GeneralUtil.htmlEncode(params.get(JiraIssuesMacro.TITLE)));
        }
    }

    public void setJiraIssueSortingManager(JiraIssueSortingManager jiraIssueSortingManager)
    {
        this.jiraIssueSortingManager = jiraIssueSortingManager;
    }

    public void setJiraIssuesColumnManager(JiraIssuesColumnManager jiraIssuesColumnManager)
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
    }
}
