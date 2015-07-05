package com.atlassian.confluence.extra.jira.model;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is a representation of the batch reponse (per JIRA server) (temporary for single issue)
 */
public class JiraBatchProcessor
{
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private List<String> issueKeys;
    private Future<Map<String, String>> futureResult;
    private Map<String, Map<String, String>> macroParameters = Maps.newHashMap();

    public List<String> getIssueKeys()
    {
        return issueKeys;
    }

    public void setIssueKeys(List<String> issueKeys)
    {
        this.issueKeys = issueKeys;
    }


    public Future<Map<String, String>> getFutureResult()
    {
        return futureResult;
    }

    public void setFutureResult(Future<Map<String, String>> futureResult)
    {
        this.futureResult = futureResult;
    }

    public Future<Map<String, Map<String, String>>> getSafeParameters()
    {
        Callable<Map<String, Map<String, String>>> callable = new Callable(){
            public Object call() throws Exception {
                while (issueKeys == null || issueKeys.size() != macroParameters.size())
                {
                    Thread.sleep(10); //do nothing
                }
                return macroParameters;
            }
        };
        return executorService.submit(callable);
    }

    public Map<String, Map<String, String>> getMacroParameters()
    {
        return macroParameters;
    }

    public void addMacroParameter(String issueKey, Map<String, String> parameters)
    {
        macroParameters.put(issueKey, parameters);
    }
}
