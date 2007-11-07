package com.atlassian.confluence.extra.jira;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Used as the key for the jira issues macro cache.
 */
public class CacheKey
{
    final String url;
    final String columns;
    final boolean showCount;
    final String template;

    public CacheKey(String url, String columns, boolean showCount, String template)
    {
        this.url = url;
        this.columns = columns;
        this.showCount = showCount;
        this.template = template;
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this,o);
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}

