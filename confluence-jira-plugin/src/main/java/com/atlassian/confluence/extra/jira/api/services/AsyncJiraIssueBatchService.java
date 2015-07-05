package com.atlassian.confluence.extra.jira.api.services;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.model.EntityServerCompositeKey;
import com.atlassian.confluence.extra.jira.model.JiraBatchProcessor;
import com.atlassian.confluence.extra.jira.model.JiraBatchResponseData;
import com.atlassian.confluence.macro.MacroExecutionException;

import java.util.Map;
import java.util.Set;

/**
 * Service responsible for sending batch request to a JIRA server and get the results
 */
public interface AsyncJiraIssueBatchService
{
    JiraBatchResponseData getAsyncBatchResults(long entityId, String serverId) throws Exception;
    JiraBatchProcessor processBatchRequest(ContentEntityObject entityObject, String serverId, Set<String> keys, ConversionContext conversionContext);
}
