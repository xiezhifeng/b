package com.atlassian.confluence.extra.jira;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The interface that defines the methods callers can invoke to set/get information about
 * columns in JIRA issues.
 */
public interface JiraIssuesColumnManager
{
    Set<String> ALL_BUILTIN_COLUMN_NAMES = Collections.unmodifiableSet(new HashSet<String>(       
            Arrays.asList(
                    "description", "environment", "key", "summary", "type", "parent",
                    "priority", "status", "version", "resolution", "security", "assignee", "reporter",
                    "created", "updated", "due", "component", "votes", "comments", "attachments",
                    "subtasks", "fixversion", "timeoriginalestimate", "timeestimate"
            ))
    );
    
    Set<String> ALL_MULTIVALUE_BUILTIN_COLUMN_NAMES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(
                    "version",
                    "component",
                    "comments",
                    "attachments",
                    "fixversion"
            ))
    );

    /**
     * Get a site specific column name to ID mapping.
     * @param jiraIssuesUrl
     * The site URL.
     * @return
     * A {@link java.util.Map} of column names to column IDs.
     */
    Map<String, String> getColumnMap(String jiraIssuesUrl);

    /**
     * Sets a site specific column name to ID mapping.
     * @param jiraIssuesUrl
     * The site URL.
     * @param columnMapping
     * A {@link java.util.Map} of column names to column IDs.
     *
     * @see {@link #getColumnMap(String)}
     */
    void setColumnMap(String jiraIssuesUrl, Map<String, String> columnMapping);

    /**
     * Checks if the specified column name is a built-in JIRA field.
     * @param columnName
     * The column name
     * @return
     * Returns <tt>true</tt> if the column name represents a JIRA built-in column; <tt>false</tt> otherwise.
     */
    boolean isColumnBuiltIn(String columnName);

    /**
     * Gets the XML key of a built-in column
     * @param columnName
     * The column name
     * @return
     * The RSS key of a builtin column. Returns <tt>null</tt> if the column is not a builtin JIRA field.
     */
    String getCanonicalFormOfBuiltInField(String columnName);

    /**
     * Checks if a built-in column is multivalue.
     * @param columnName
     * The column name
     * @return
     * Returns <tt>true</tt> if the column represents a JIRA built-in field <em>and</em> can have multiple values;
     * <tt>false</tt> otherwise.
     */
    boolean isBuiltInColumnMultivalue(String columnName);
}
