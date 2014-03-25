package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.model.JiraBatchRequestData;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SingleJiraIssuesThreadLocalAccessor
{
    private static final ThreadLocal<Map<String, JiraBatchRequestData>> jiraBatchRequestDataMapThreadLocal = new ThreadLocal<Map<String, JiraBatchRequestData>>();

    private static final ThreadLocal<Boolean> batchProcessed = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue()
        {
            return Boolean.FALSE;
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleJiraIssuesThreadLocalAccessor.class);
    /**
     *
     * @param serverId
     * @param jiraBatchRequestData
     */
    public static void putJiraBatchRequestData(String serverId, JiraBatchRequestData jiraBatchRequestData)
    {
        Map<String, JiraBatchRequestData> stringJiraBatchRequestDataMap = jiraBatchRequestDataMapThreadLocal.get();
        if (stringJiraBatchRequestDataMap != null)
        {
            stringJiraBatchRequestDataMap.put(serverId, jiraBatchRequestData);
        }
    }

    /**
     * Initialise the jiraBatchRequestDataMapThreadLocal for the current thread
     */
    public static void init()
    {
        if (jiraBatchRequestDataMapThreadLocal.get() == null)
        {
            jiraBatchRequestDataMapThreadLocal.set(Maps.<String, JiraBatchRequestData>newHashMap());
        }
    }

    /**
     * Clean up the jiraBatchRequestDataMapThreadLocal for the current thread.
     */
    public static void dispose()
    {
        jiraBatchRequestDataMapThreadLocal.remove();
        batchProcessed.remove();
    }

    public static JiraBatchRequestData getJiraBatchRequestData(String serverId)
    {
        Map<String, JiraBatchRequestData> stringJiraBatchRequestDataMap = jiraBatchRequestDataMapThreadLocal.get();
        if (stringJiraBatchRequestDataMap != null)
        {
            return stringJiraBatchRequestDataMap.get(serverId);
        }
        return null;
    }

    public static void setBatchProcessed(Boolean processed)
    {
        batchProcessed.set(processed);
    }

    public static Boolean isBatchProcessed()
    {
        return batchProcessed.get();
    }
}
