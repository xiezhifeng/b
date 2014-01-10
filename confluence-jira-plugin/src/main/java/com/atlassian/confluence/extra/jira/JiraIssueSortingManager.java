package com.atlassian.confluence.extra.jira;

import java.util.Map;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.macro.MacroExecutionException;

public interface JiraIssueSortingManager {


    /**
     * Get request data support for sorting.
     * @param parameters of jira issue macro
     * @param requestData for Jira issue macro
     * @param requestType is JQL or URL
     * @param jiraColumns all jira columns retrieve from REST API (/rest/api/2/field)
     * @param conversionContext JIM context
     * @param applink ApplicationLink
     * @return request data after added infomation for sorting
     * @throws MacroExecutionException if have any error otherwise
     */
    String getRequestDataForSorting(Map<String, String> parameters, String requestData, Type requestType, Map<String, JiraColumnInfo> jiraColumns, ConversionContext conversionContext, ApplicationLink applink) throws MacroExecutionException;

}
