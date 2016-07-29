package com.atlassian.confluence.extra.jira.helper;

import com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JiraIssueSortableHelper
{

    public static final long SUPPORT_JIRA_BUILD_NUMBER = 6251L; // JIRA v6.2-OD-08
    public static final String SPACE = " ";
    public static final String DOUBLE_QUOTE = "\"";
    public static final String SINGLE_QUOTE = "\'";
    
    private static final List<String> DEFAULT_RSS_FIELDS = Arrays.asList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due");
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";
    private static final String COMMA = ",";

    private JiraIssueSortableHelper()
    {
        
    }

    /**
     * Reorder columns for sorting.
     * @param orderType can be "ASC" or "DESC"
     * @param clauseName for sorting
     * @param orderQuery in JQL
     * @return new order columns in JQL
     */
    public static String reoderColumns(String orderType, String clauseName, String orderQuery, Map<String, JiraColumnInfo> jiraColumns)
    {
        String[] orderColumns = StringUtils.split(orderQuery, COMMA);
        List<String> reOrderColumns = Lists.newArrayList();
        for (String col : orderColumns)
        {
            col = StringUtils.remove(col.trim(), DOUBLE_QUOTE);
            String columnName = col;
            String orderTypeColumn = StringUtils.EMPTY;
            if (StringUtils.endsWithIgnoreCase(col, SPACE + ASC) || StringUtils.endsWithIgnoreCase(col, SPACE + DESC))
            {
                String[] columnPart = StringUtils.split(col, SPACE);
                columnName = columnPart[0];
                orderTypeColumn = SPACE + columnPart[1];
            }
            if (!isSameColumn(columnName, clauseName, jiraColumns))
            {
                reOrderColumns.add(columnName + orderTypeColumn);
            }
        }
        reOrderColumns.add(0, DOUBLE_QUOTE + JiraUtil.escapeDoubleQuote(clauseName) + DOUBLE_QUOTE + SPACE + orderType);

        return StringUtils.join(reOrderColumns, COMMA);
    }

    /**
     * Gets column names base on column parameter from JIM.
     * @param columnsParameter columns parameter from JIM
     * @return a list of column names
     */
    public static List<String> getColumnNames(String columnsParameter, ImmutableMap<String, ImmutableSet<String>> i18nColumnNames)
    {
        if (StringUtils.isBlank(columnsParameter)) {
            return DEFAULT_RSS_FIELDS;
        }

        final List<String> columnNames = new ArrayList<>();
        final List<String> keys = Arrays.asList(StringUtils.split(columnsParameter, ",;"));

        for (String key : keys)
        {
            if (StringUtils.isNotBlank(key))
            {
                if(DefaultJiraIssuesColumnManager.matchColumnNameFromString(DefaultJiraIssuesColumnManager.COLUMN_EPIC_NAME, key, i18nColumnNames) ||
                        DefaultJiraIssuesColumnManager.matchColumnNameFromString(DefaultJiraIssuesColumnManager.COLUMN_EPIC_LINK, key, i18nColumnNames)) {
                    if (!DefaultJiraIssuesColumnManager.matchColumnNameFromList(DefaultJiraIssuesColumnManager.COLUMN_EPIC_LINK, columnNames, i18nColumnNames)) {
                        // Should only be one item in this set.
                        columnNames.add(i18nColumnNames.get(DefaultJiraIssuesColumnManager.COLUMN_EPIC_LINK_DISPLAY).iterator().next());
                    }
                } else {
                    columnNames.add(key);
                }
            }
        }

        return columnNames.isEmpty() ? DEFAULT_RSS_FIELDS : columnNames;
    }

    public static boolean isJiraSupportedOrder(JiraServerBean jiraServer)
    {
        return jiraServer != null && jiraServer.getBuildNumber() >= JiraIssueSortableHelper.SUPPORT_JIRA_BUILD_NUMBER;
    }

    private static boolean isSameColumn(String column, String aliasRefColumn, Map<String, JiraColumnInfo> jiraColumns)
    {
        if (StringUtils.equalsIgnoreCase(column, aliasRefColumn))
        {
            return true;
        }

        for (JiraColumnInfo jiraColumnInfo : jiraColumns.values())
        {
            if (jiraColumnInfo.getClauseNames().contains(aliasRefColumn))
            {
                return jiraColumnInfo.getClauseNames().contains(column);
            }
        }
        return false;
    }
}

