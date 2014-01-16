package com.atlassian.confluence.extra.jira.model;

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

    @SerializedName("clauseNames")
    private List<String> clauseNames;

    private boolean sort;

    @SerializedName("custom")
    private boolean custom;

    public String getRssKey()
    {
        return rssKey;
    }

    public void setRssKey(String rssKey)
    {
        this.rssKey = rssKey;
    }

    public void setTitle(String title)
    {
        this.title = title;
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

    public JiraColumnInfo(String rssKey, String title, List<String> clauseNames)
    {
        this(rssKey, title);
        this.clauseNames = clauseNames;
    }

    public JiraColumnInfo(String rssKey, String title, boolean sort)
    {
        this(rssKey, title);
        this.sort = sort;
    }
    
    public JiraColumnInfo(String rssKey, String title, List<String> clauseNames, boolean sort)
    {
        this(rssKey, title, clauseNames);
        this.sort = sort;
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
            return this.rssKey.equalsIgnoreCase((String) obj);
        }
        else if (obj instanceof JiraColumnInfo)
        {
            JiraColumnInfo otherJiraColumnInfo = (JiraColumnInfo) obj;
            return this.rssKey.equalsIgnoreCase(otherJiraColumnInfo.rssKey);
        }
        return false;
    }

    public int hashCode()
    {
        return this.rssKey.hashCode();
    }

    public List<String> getClauseNames()
    {
        return clauseNames;
    }

    public void setClauseName(List<String> clauseNames)
    {
        this.clauseNames = clauseNames;
    }

    public boolean isSort()
    {
        return sort;
    }

    public String getPrimaryClauseName()
    {
        return this.clauseNames != null && this.clauseNames.size() > 0 ? this.clauseNames.get(0) : "";
    }

    public boolean isCustom()
    {
        return this.custom;
    }
}
