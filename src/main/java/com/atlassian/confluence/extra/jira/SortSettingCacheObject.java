package com.atlassian.confluence.extra.jira;

import java.io.Serializable;

public class SortSettingCacheObject implements Serializable
{
    private boolean enableSort;
    /* time since last made request to jira to determine if sorting should be enabled */
    private long timeRefreshed;

    public SortSettingCacheObject()
    {
    }

    public boolean isEnableSort()
    {
        return enableSort;
    }

    public void setEnableSort(boolean enableSort)
    {
        this.enableSort = enableSort;
    }

    public long getTimeRefreshed()
    {
        return timeRefreshed;
    }

    public void setTimeRefreshed(long timeRefreshed)
    {
        this.timeRefreshed = timeRefreshed;
    }
}
