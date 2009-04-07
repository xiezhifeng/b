package com.atlassian.confluence.extra.jira;

/**
 * Utilities for interfacing with Seraph
 */
public class SeraphUtils
{
    private SeraphUtils()
    {

    }

    /**
     * Detect whether Seraph username and password in present in a URL string
     * @param url URL to search
     * @return True if both a username and password are present in the URL
     */
    public static boolean isUserNamePasswordProvided(String url)
    {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.indexOf("os_username") != -1 && lowerUrl.indexOf("os_password") != -1;
    }
}
