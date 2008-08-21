package com.atlassian.confluence.extra.jira;

public class SortSettingCacheObject
{
    private boolean enableSort;

    /* time since last made request to jira to determine if sorting should be enabled */
    private long timeRefreshed;

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
