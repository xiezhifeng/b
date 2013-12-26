package com.atlassian.confluence.extra.jira;

import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.SerializedName;


public class JiraColumnInfo
{
    private static final List<String> NO_WRAPPED_TEXT_FIELDS = Arrays.asList("key", "type", "priority", "status", "created", "updated", "due");
    private static final String CLASS_NO_WRAP = "columns nowrap";
    private static final String CLASS_WRAP = "columns";

    @SerializedName("name")
    private String title;

    @SerializedName("id")
    private String rssKey;

    private boolean orderable;

    public String getRssKey() {
        return rssKey;
    }

    public void setRssKey(String rssKey) {
        this.rssKey = rssKey;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOrderable(boolean orderable) {
        this.orderable = orderable;
    }

    public JiraColumnInfo()
    {
    }

    public JiraColumnInfo(String rssKey)
    {
        this(rssKey, rssKey);
    }

    public JiraColumnInfo(String rssKey, String title)
    {
        this.rssKey = rssKey;
        this.title = title;
    }

    public JiraColumnInfo(String rssKey, String title, boolean orderable)
    {
        this(rssKey, title);
        this.orderable = orderable;
    }

    public boolean isOrderable()
    {
        return this.orderable;
    }

    public String getTitle()
    {
        return title;
    }

    public String getKey()
    {
        return this.rssKey;
    }

    
    public String getHtmlClassName()
    {
        return (shouldWrap() ? CLASS_WRAP : CLASS_NO_WRAP);
    }

    public boolean shouldWrap()
    {
        return !NO_WRAPPED_TEXT_FIELDS.contains(getKey().toLowerCase());
    }

    public String toString()
    {
        return getKey();
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof String)
        {
            String str = (String) obj;
            return this.rssKey.equalsIgnoreCase(str);
        }
        else if (obj instanceof JiraColumnInfo)
        {
            JiraColumnInfo that = (JiraColumnInfo) obj;
            return this.rssKey.equalsIgnoreCase(that.rssKey);
        }
        return false;
    }

    public int hashCode()
    {
        return this.rssKey.hashCode();
    }

}
