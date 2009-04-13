package com.atlassian.confluence.extra.jira;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class DefaultJiraIssuesColumnManager implements JiraIssuesColumnManager
{
    private final JiraIssuesSettingsManager jiraIssuesSettingsManager;

    public DefaultJiraIssuesColumnManager(JiraIssuesSettingsManager jiraIssuesSettingsManager)
    {
        this.jiraIssuesSettingsManager = jiraIssuesSettingsManager;
    }

    public Map<String, String> getColumnMap(String jiraIssuesUrl)
    {
        return jiraIssuesSettingsManager.getColumnMap(jiraIssuesUrl);
    }

    public void setColumnMap(String jiraIssuesUrl, Map<String, String> columnMapping)
    {
        jiraIssuesSettingsManager.setColumnMap(jiraIssuesUrl, columnMapping);
    }

    public boolean isColumnBuiltIn(String columnName)
    {
        for (String builtinName : ALL_BUILTIN_COLUMN_NAMES)
            if (StringUtils.equalsIgnoreCase(builtinName, columnName))
                return true;

        return false;
    }

    public String getCanonicalFormOfBuiltInField(String columnName)
    {
        for (String builtinName : ALL_BUILTIN_COLUMN_NAMES)
            if (StringUtils.equalsIgnoreCase(builtinName, columnName))
                return builtinName;

        return columnName;
    }

    public boolean isBuiltInColumnMultivalue(String columnName)
    {
        for (String multivalueBuiltInColumnName : ALL_MULTIVALUE_BUILTIN_COLUMN_NAMES)
            if (StringUtils.equalsIgnoreCase(multivalueBuiltInColumnName, columnName))
                return true;

        return false;
    }
}
