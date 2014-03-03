package com.atlassian.confluence.extra.jira;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.google.common.collect.ImmutableMap;

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
                    "created", "updated", "due", "component", "components", "votes", "comments", "attachments",
                    "subtasks", "fixversion", "timeoriginalestimate", "timeestimate"
            ))
    );
    
    Set<String> ALL_MULTIVALUE_BUILTIN_COLUMN_NAMES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(
                    "version",
                    "component",
                    "comments",
                    "attachments",
                    "fixversion",
                    "fixVersion"
            ))
    );

    Set<String> SUPPORT_SORTABLE_COLUMN_NAMES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(
                    "key", "summary", "type", "created", "updated", "due", "assignee", "reporter", "priority", "status",
                    "resolution", "version", "security", "watches", "components", "description", "environment", "fixVersion", "labels", "lastviewed",
                    "timeoriginalestimate", "progress", "project", "timeestimate", "resolved", "subtasks", "timespent", "votes", "workratio", "resolutiondate"
            ))
    );

    Map<String, String> XML_COLUMN_KEYS_MAPPING = new ImmutableMap.Builder<String, String>().put("version", "affectedVersion")
            .put("security", "level")
            .put("watches", "watchers")
            .put("type", "issuetype")
            .build();



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

    /**
     * Gets all fields in Jira via REST API /rest/api/2/field and keep it in catch for next use. 
     * @param appLink applicationLink to Jira
     * @return a Map of column info where the key is the Id and the value is an JiraColumnInfo instance or an empty map.
     */
    Map<String, JiraColumnInfo> getColumnsInfoFromJira(ApplicationLink appLink);

    /**
     *  Gets column info from JIRA and provides sorting ability.
     * @param params JIRA issue macro parameters
     * @param columns retrieve from REST API 
     * @param applink use to detect which version of JIRA
     * @return JIRA column info
     */
    List<JiraColumnInfo> getColumnInfo(Map<String, String> params, Map<String, JiraColumnInfo> columns, ApplicationLink applink);

    /**
     * Get columnKey is mapped between JIRA and JIM to support sortable ability.
     * @param columnKey is key from JIM
     * @return key has mapped.
     */
    String getColumnMapping(String columnKey, Map<String, String> map);

}
