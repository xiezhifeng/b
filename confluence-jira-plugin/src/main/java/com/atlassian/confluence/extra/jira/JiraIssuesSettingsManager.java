package com.atlassian.confluence.extra.jira;

import java.util.Map;

/**
 * The interface that defines the methods to persist certain settings
 * related to the JIRA issues macro.
 */
public interface JiraIssuesSettingsManager
{

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
     * See {@link #getColumnMap(String)}
     */
    void setColumnMap(String jiraIssuesUrl, Map<String, String> columnMapping);


    /**
     * Gets a {@link java.util.Map} of JIRA issue types to icons.
     * @return
     * A {@link java.util.Map} where the key represents the issue type (e.g. &quot;Bug&quot;) to
     * the icon file name (e.g &quot;bug.gif&quot;).
     */
    Map<String, String> getIconMapping();

    /**
     * Sets the JIRA issues to icons mapping
     * @param iconMapping
     * The {@link java.util.Map} representing the mapping.
     * See {@link #getIconMapping()}
     */
    void setIconMapping(Map<String, String> iconMapping);
}
