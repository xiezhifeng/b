package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.macro.MacroExecutionException;

import java.util.Map;

public interface JiraIssueSortingManager
{


    /**
     * Get request data support for sorting.
     * @param parameters of jira issue macro
     * @param jiraRequestData for Jira issue macro
     * @param jiraColumns all jira columns retrieve from REST API (/rest/api/2/field)
     * @param conversionContext JIM context
     * @param applink ApplicationLink
     * @return request data after added infomation for sorting
     * @throws MacroExecutionException if have any error otherwise
     */
    String getRequestDataForSorting(Map<String, String> parameters, JiraRequestData jiraRequestData, Map<String, JiraColumnInfo> jiraColumns, ConversionContext conversionContext, ApplicationLink applink) throws MacroExecutionException;

}
