package com.atlassian.confluence.extra.jira;

import java.util.List;
import java.util.Map;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.macro.MacroExecutionException;

/**
 * 
 * @author hao.hotrung
 *
 */
public interface JiraIssueSortingManager {

    /**
     *  Gets column info from JIRA and provides sorting ability.
     * @param params JIRA issue macro parameters
     * @param columns retrieve from REST API 
     * @return jira column info
     */
    List<JiraColumnInfo> getColumnInfo(Map<String, String> params, Map<String, JiraColumnInfo> columns);

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
