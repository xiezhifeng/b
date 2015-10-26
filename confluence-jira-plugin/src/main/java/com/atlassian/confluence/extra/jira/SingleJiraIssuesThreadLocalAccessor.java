package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.model.EntityServerCompositeKey;
import com.atlassian.confluence.extra.jira.model.JiraBatchRequestData;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SingleJiraIssuesThreadLocalAccessor
{
    private static final ThreadLocal<Map<EntityServerCompositeKey, JiraBatchRequestData>> jiraBatchRequestDataMapThreadLocal = new ThreadLocal<Map<EntityServerCompositeKey, JiraBatchRequestData>>();

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
     * @param entityServerCompositeKey
     * @param jiraBatchRequestData
     */
    public static void putJiraBatchRequestData(EntityServerCompositeKey entityServerCompositeKey, JiraBatchRequestData jiraBatchRequestData)
    {
        Map<EntityServerCompositeKey, JiraBatchRequestData> jiraBatchRequestDataMap = jiraBatchRequestDataMapThreadLocal.get();
        if (jiraBatchRequestDataMap != null)
        {
            jiraBatchRequestDataMap.put(entityServerCompositeKey, jiraBatchRequestData);
        }
    }

    /**
     * Initialise the jiraBatchRequestDataMapThreadLocal for the current thread
     */
    public static void init()
    {
        jiraBatchRequestDataMapThreadLocal.set(Maps.<EntityServerCompositeKey, JiraBatchRequestData>newHashMap());
        batchProcessedMapThreadLocal.set(Maps.<Long, Boolean>newHashMap());
    }

    public static JiraBatchRequestData getJiraBatchRequestData(EntityServerCompositeKey entityServerCompositeKey)
    {
        Map<EntityServerCompositeKey, JiraBatchRequestData> jiraBatchRequestDataMap = jiraBatchRequestDataMapThreadLocal.get();
        if (jiraBatchRequestDataMap != null)
        {
            return jiraBatchRequestDataMap.get(entityServerCompositeKey);
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
