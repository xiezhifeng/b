package com.atlassian.confluence.extra.jira;

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
       return ALL_BUILTIN_COLUMN_NAMES.contains(columnName.toLowerCase());
    }

    public String getCanonicalFormOfBuiltInField(String columnName)
    {   
    	if (columnName.equalsIgnoreCase("fixversion"))
        {
            return "fixVersion";
        } else if (columnName.equalsIgnoreCase("fixversions")) {
            return "fixVersion";
        } else if (columnName.equalsIgnoreCase("versions")) {
            return "version";
        } else if (columnName.equalsIgnoreCase("components")) {
        	return "component";
        }
        if (isColumnBuiltIn(columnName))
        {
            return columnName.toLowerCase();
        }
        return columnName;
    }

    public boolean isBuiltInColumnMultivalue(String columnName)
    {
        return ALL_MULTIVALUE_BUILTIN_COLUMN_NAMES.contains(columnName.toLowerCase());           
    }
}
