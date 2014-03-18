package com.atlassian.confluence.extra.jira.api.services;

import org.jdom.Element;

import java.util.Map;
import java.util.Set;

public interface JiraIssueBatchService {

    static final String ELEMENT_MAP = "elementMap";
    static final String JIRA_SERVER_URL = "jiraServerUrl";

    Map<String, Object> getBatchResults(Map<String, String> macroParameters, Set<String> keys);
}
