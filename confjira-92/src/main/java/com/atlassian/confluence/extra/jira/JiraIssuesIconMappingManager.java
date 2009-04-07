package com.atlassian.confluence.extra.jira;

import java.util.Map;

/**
 * Defines methods that can be called to manipulate the JIRA issue type to icon mapping.
 */
public interface JiraIssuesIconMappingManager
{
    /**
     * Gets a {@link java.util.Map} of JIRA issue types to icons.
     * @return
     * A {@link java.util.Map} where the key represents the issue type (e.g. &quot;Bug&quot;) to
     * the icon file name (e.g &quot;bug.gif&quot;).
     */
    Map<String, String> getBaseIconMapping();

    /**
     * Gets a {@link java.util.Map} of JIRA issue types to icons. This is different from
     * {@link #getBaseIconMapping()} because the icon paths will be in full (not just the file name).
     *
     * @param link
     * The link which will be used to calculate the full path of the link icon paths.
     *
     * @return
     * A {@link java.util.Map} where the key represents the issue type (e.g. &quot;Bug&quot;) to
     * the icon path name (e.g &quot;http://foo/images/icon/bug.gif&quot;).
     */
    Map<String, String> getFullIconMapping(String link);

    /**
     * Adds a new issue type to icon mapping.
     * @param issueType
     * The issue type.
     * @param iconFileName
     * The icon file name (e.g &quot;bug.gfi&quot;)
     */
    void addBaseIconMapping(String issueType, String iconFileName);

    /**
     * Removes a issue type to icon mapping
     * @param issueType
     * The issue type.
     */
    void removeBaseIconMapping(String issueType);

    /**
     * Checks if a particular issue type has an icon mapping.
     * @param issueType
     * The issue type.
     * @return
     * Returns <tt>true</tt> if the issue type is mapped to an icon; <tt>false</tt> otherwise.
     */
    boolean hasBaseIconMapping(String issueType);

    /**
     * Gets the icon file name based on the issue type it is mapped to.
     * @param issueType
     * The issue type.
     * @return
     * The icon file name (e.g &quot;bug.gif&quot;). This will be <tt>null</tt> if the issue type
     * is not mapped to any icon.
     */
    String getIconFileName(String issueType);
}
