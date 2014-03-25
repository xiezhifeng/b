package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.model.JiraBatchRequestData;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SingleJiraIssuesThreadLocalAccessor
{
    private static final ThreadLocal<Map<String, JiraBatchRequestData>> jiraBatchRequestDataMapThreadLocal = new ThreadLocal<Map<String, JiraBatchRequestData>>();

    private static final ThreadLocal<Map<Long, Boolean>> batchProcessedMapThreadLocal = new ThreadLocal<Map<Long, Boolean>>()
    {
        @Override
        protected Map<Long, Boolean> initialValue()
        {
            return Maps.newHashMap();
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
        batchProcessedMapThreadLocal.remove();
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

    public static void setBatchProcessedMapThreadLocal(Long contentId, Boolean processed)
    {
        Map<Long, Boolean> batchProcessedMap = batchProcessedMapThreadLocal.get();
        if (batchProcessedMap != null)
        {
            batchProcessedMap.put(contentId, processed);
        }
    }

    public static Boolean isBatchProcessed(Long contentId)
    {
        Map<Long, Boolean> batchProcessMap = batchProcessedMapThreadLocal.get();
        if (batchProcessMap != null)
        {
            Boolean processed = batchProcessMap.get(contentId);
            return processed == null ? Boolean.FALSE : processed;
        }
        return Boolean.FALSE;
    }
}
