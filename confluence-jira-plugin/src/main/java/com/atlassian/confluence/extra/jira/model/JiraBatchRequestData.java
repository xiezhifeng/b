package com.atlassian.confluence.extra.jira.model;

import com.atlassian.confluence.macro.MacroExecutionException;
import org.jdom.Element;

import java.util.Map;

/**
 * This is is a representation of the JIRA batch request for single issues
 */
public class JiraBatchRequestData {

    private Map<String, Element> serverElementMap; // Map of (JIRA Issue Key, JDOM Element) pairs

    private String jiraServerUrl;

    private MacroExecutionException macroExecutionException;

    public Map<String, Element> getServerElementMap() {
        return serverElementMap;
    }

    public void setServerElementMap(Map<String, Element> serverElementMap) {
        this.serverElementMap = serverElementMap;
    }

    public String getJiraServerUrl() {
        return jiraServerUrl;
    }

    public void setJiraServerUrl(String jiraServerUrl) {
        this.jiraServerUrl = jiraServerUrl;
    }

    public MacroExecutionException getMacroExecutionException() {
        return macroExecutionException;
    }

    public void setMacroExecutionException(MacroExecutionException macroExecutionException) {
        this.macroExecutionException = macroExecutionException;
    }
}
