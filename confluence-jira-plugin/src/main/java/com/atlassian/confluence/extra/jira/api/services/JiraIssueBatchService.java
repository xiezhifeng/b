package com.atlassian.confluence.extra.jira.api.services;

import org.jdom.Element;

import java.util.Map;
import java.util.Set;

public interface JiraIssueBatchService {

    Map<String, Element> getBatchResults(Map<String, String> macroParameters, Set<String> keys);
}
