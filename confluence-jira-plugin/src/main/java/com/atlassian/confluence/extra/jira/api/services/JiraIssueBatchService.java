package com.atlassian.confluence.extra.jira.api.services;

import com.atlassian.confluence.macro.MacroExecutionException;

import java.util.Map;
import java.util.Set;

public interface JiraIssueBatchService {

    Map<String, String> getBatchResults(Map<String, String> macroParameters, Set<String> keys);
}
