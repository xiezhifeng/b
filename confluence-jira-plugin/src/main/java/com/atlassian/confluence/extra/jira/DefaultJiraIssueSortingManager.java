package com.atlassian.confluence.extra.jira;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;

public class DefaultJiraIssueSortingManager implements JiraIssueSortingManager
{
    private JiraIssuesColumnManager jiraIssuesColumnManager;
    private JiraIssuesManager jiraIssuesManager;

    public DefaultJiraIssueSortingManager(JiraIssuesColumnManager jiraIssuesColumnManager, JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.jiraIssuesManager = jiraIssuesManager;
    }

    @Override
    public String getRequestDataForSorting(Map<String, String> parameters, String requestData, Type requestType, Map<String, JiraColumnInfo> jiraColumns, ConversionContext conversionContext, ApplicationLink applink) throws MacroExecutionException
    {
        String orderColumnName = (String) conversionContext.getProperty("orderColumnName");
        String order = (String) conversionContext.getProperty("order");
        // Disable caching Jira issue.
        parameters.put("cache", "off");
        if (StringUtils.isBlank(orderColumnName))
        {
            return requestData;
        }
        String clauseName = getClauseName(parameters, jiraColumns, orderColumnName);
        switch (requestType)
        {
            case URL:
                return getUrlSortRequest(requestData, clauseName, order, JiraUtil.getMaximumIssues(parameters.get("maximumIssues")), applink);
            case JQL:
                return getJQLSortRequest(requestData, clauseName, order); 
            default:
                return requestData;
        }
    }

    private String getClauseName(final Map<String, String> parameters, final Map<String, JiraColumnInfo> jiraColumns, final String orderColumnName)
    {
        List<JiraColumnInfo> columns = jiraIssuesColumnManager.getColumnInfo(parameters, jiraColumns);
        for (JiraColumnInfo columnInfo : columns)
        {
            if (columnInfo.getTitle().equalsIgnoreCase(orderColumnName))
            {
                return jiraIssuesColumnManager.getColumnMapping(columnInfo.getPrimaryClauseName());
            }
        }
        return StringUtils.EMPTY;
    }

    private String getUrlSortRequest(String requestData, String clauseName, String order, int maximumIssues, ApplicationLink applink) throws MacroExecutionException
    {
        StringBuilder urlSort = new StringBuilder();
        String jql = StringUtils.EMPTY;
        if (JiraJqlHelper.isFilterType(requestData))
        {
            jql = JiraJqlHelper.getJQLFromFilter(applink, requestData, jiraIssuesManager, jiraIssuesColumnManager.getI18NBean());
        }
        if (StringUtils.isNotBlank(jql))
        {
            StringBuffer sf = new StringBuffer(JiraUtil.normalizeUrl(applink.getRpcUrl()));
            sf.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?jqlQuery=");
            sf.append(JiraUtil.utf8Encode(jql)).append("&tempMax=" + maximumIssues);
            requestData = sf.toString();
        }
        Matcher matcher = JiraJqlHelper.XML_SORTING_PATTERN.matcher(requestData);
        if (matcher.find())
        {
            jql = JiraUtil.utf8Decode(JiraJqlHelper.getValueByRegEx(requestData, JiraJqlHelper.XML_SORTING_PATTERN, 2));
            String tempMax = JiraJqlHelper.getValueByRegEx(requestData, JiraJqlHelper.XML_SORTING_PATTERN, 3);
            String url = requestData.substring(0, matcher.end(1) + 1);
            Matcher orderMatch = JiraJqlHelper.SORTING_PATTERN.matcher(jql);
            String orderData;
            if (orderMatch.find())
            {
                String orderColumns = jql.substring(orderMatch.end() - 1, jql.length());
                jql = jql.substring(0, orderMatch.end() - 1);
                // check orderColumn is exist on jql or not.
                // first check column key
                orderData = JiraIssueSortableHelper.reoderColumns(order, clauseName, orderColumns);
            }
            else // JQL does not have order by clause.
            {
                orderData = " ORDER BY " + clauseName + JiraIssueSortableHelper.SPACE + order;
            }
            urlSort.append(url + JiraUtil.utf8Encode(jql + orderData) + "&tempMax=" + tempMax);
        }
        return urlSort.toString();
    }

    private String getJQLSortRequest(String requestData, String clauseName, String order) throws MacroExecutionException
    {
        StringBuilder urlSort = new StringBuilder();
        Matcher matcher = JiraJqlHelper.SORTING_PATTERN.matcher(requestData);
        if (matcher.find())
        {
            String orderColumns = requestData.substring(matcher.end() - 1, requestData.length());
            // check orderColumn is exist on jql or not.
            // first check column key
            orderColumns = JiraIssueSortableHelper.reoderColumns(order, clauseName, orderColumns);
            urlSort.append(requestData.substring(0, matcher.end() - 1) + orderColumns);
        }
        else // JQL does not have order by clause.
        {
            requestData = requestData + " ORDER BY " + clauseName + JiraIssueSortableHelper.SPACE + order;
            urlSort.append(requestData);
        }
        return urlSort.toString();
    }
}
