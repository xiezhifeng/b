package com.atlassian.confluence.extra.jira.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This is a representation of the batch reponse (per JIRA server) (temporary for single issue)
 */
public class JiraBatchProcessor
{
    private Future<Map<String, List<String>>> futureResult;

    public Future<Map<String, List<String>>> getFutureResult()
    {
        return futureResult;
    }

    public void setFutureResult(Future<Map<String, List<String>>> futureResult)
    {
        this.futureResult = futureResult;
    }

}
