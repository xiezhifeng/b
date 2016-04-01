package com.atlassian.confluence.extra.jira.model;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class JiraColumnInfo
{
    private static final List<String> NO_WRAPPED_TEXT_FIELDS = Arrays.asList("key", "type", "priority", "status", "created", "updated", "due");
    private static final String CLASS_NO_WRAP = "columns nowrap";
    private static final String CLASS_WRAP = "columns";
    private static final String URL_CUSTOM_FIELD_TYPE = "com.atlassian.jira.plugin.system.customfieldtypes:url";

    public static class Schema
    {
        @SerializedName("type")
        private String type;

        @SerializedName("custom")
        private String custom;

        @SerializedName("customId")
        private int customid;

        public Schema()
        {
        }

        public Schema(String type, String custom, int customid)
        {
            this.type = type;
            this.custom = custom;
            this.customid = customid;
        }
    }

    @SerializedName("name")
    private String title;

    @SerializedName("id")
    private String rssKey;

    @SerializedName("clauseNames")
    private List<String> clauseNames;

    private boolean sortable;

    @SerializedName("custom")
    private boolean custom;

    @SerializedName("navigable")
    private boolean navigable;

    @SerializedName("schema")
    private Schema schema;
    
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

    public JiraColumnInfo(String rssKey, String title, boolean sortable)
    {
        this(rssKey, title);
        this.sortable = sortable;
    }
    
    public JiraColumnInfo(String rssKey, String title, List<String> clauseNames, boolean sortable)
    {
        this(rssKey, title, clauseNames);
        this.sortable = sortable;
    }

    public JiraColumnInfo(String rssKey, String title, List<String> clauseNames, boolean sortable, Schema schema)
    {
        this(rssKey, title, clauseNames);
        this.sortable = sortable;
        this.schema = schema;
    }

    public String getTitle()
    {
        return title;
    }

    public String getKey()
    {
        return this.rssKey;
    }

    public Schema getSchema() { return this.schema; }

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
        return this.clauseNames;
    }

    public void setClauseName(List<String> clauseNames)
    {
        this.clauseNames = clauseNames;
    }

    public boolean isSortable()
    {
        return sortable;
    }

    public String getPrimaryClauseName()
    {
        return this.clauseNames != null && !this.clauseNames.isEmpty() ? this.clauseNames.get(0) : "";
    }

    public boolean isCustom()
    {
        return this.custom;
    }

    public boolean isNavigable()
    {
        return this.navigable;
    }

    public boolean isUrlColumn()
    {
        return schema != null && URL_CUSTOM_FIELD_TYPE.equals(schema.custom);
    }
}
