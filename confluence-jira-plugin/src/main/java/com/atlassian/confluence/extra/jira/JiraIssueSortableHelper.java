package com.atlassian.confluence.extra.jira;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.util.i18n.I18NBean;

public class JiraIssueSortableHelper
{

    public static long SUPPORT_JIRA_BUILD_NUMBER = 7000L; // should be change to build number of JIRA when it takes the fix into account.

    private static final int PARAM_POSITION_1 = 1;
    private static final int DEFAULT_NUMBER_OF_ISSUES = 20;
    private static final int MAXIMUM_ISSUES = 1000;
    private static final List<String> DEFAULT_RSS_FIELDS = Arrays.asList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due");
    private static final String PROP_KEY_PREFIX = "jiraissues.column.";
    private static final String XML_SEARCH_REQUEST_URI = "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";
    /**
     * Check if columnName or Column Key is exist in orderLolumns.
     * @param columnName will be checked
     * @param columnKey will be checked
     * @param orderColumns in JQL
     * @return existColumnore
     */
    public static String checkOrderColumnExistJQL(String columnName, String columnKey, String orderColumns)
    {
        String existColumn = "";
        if (orderColumns.trim().toLowerCase().contains(columnKey))
        {
            existColumn = columnKey;
        } 
        else if (orderColumns.trim().toLowerCase().contains(columnName.toLowerCase()))
        {
            existColumn = columnName;
        }
        else if (orderColumns.trim().equalsIgnoreCase(columnKey))
        {
            existColumn = columnKey;
        }
        return existColumn;
    }

    /**
     * Reorder columns for sorting.
     * @param order can be "ASC" or "DESC"
     * @param columnKey for sorting
     * @param existColumn in orderColumns
     * @param orderQuery in JQL
     * @return new order columns in JQL
     */
    public static String reoderColumns(String order, String columnKey, String orderColumnName, String orderQuery)
    {
        String existColumn = JiraIssueSortableHelper.checkOrderColumnExistJQL(orderColumnName, columnKey, orderQuery);
        if (StringUtils.isBlank(existColumn))
        {
            // order column does not exist. Should put order column with the highest priority.
            // EX: order column is key with asc in order. And jql= project = conf order by summary asc.
            // Then jql should be jql= project = conf order by key acs, summaryasc.
            return " \"" + columnKey + "\" " + (StringUtils.isBlank(order) ? "ASC " : order) + (StringUtils.isNotBlank(orderQuery) ? "," + orderQuery : "");
        }
        // calculate position column is exist.
        List<String> orderQueries = Arrays.asList(orderQuery.split(","));
        int size = orderQueries.size();
        if (size == 1)
        {
            return " \"" + columnKey + "\" " + order;
        }
        if (size > 1)
        {
            for (int i = 0; i < size; i++)
            { // order by key desc, summary asc
                if (orderQueries.get(i).contains(existColumn))
                {
                    List<String> result = new ArrayList<String>();
                    String colData = orderQueries.get(i);
                    if (colData.toUpperCase().contains("ASC"))
                    {
                        result.add(colData.toUpperCase().replace("ASC", order));
                    }
                    else if (colData.toUpperCase().contains("DESC"))
                    {
                        result.add(colData.toUpperCase().replace("DESC", order));
                    }
                    else
                    {
                        result.add(" \"" + colData + "\" " + order);
                    }
                    for (String col : orderQueries)
                    {
                        if (!col.equalsIgnoreCase(colData))
                        {
                            result.add(col);
                        }
                    }
                    orderQuery = StringUtils.join(result, ",");
                    break;
                }
            }
        }
        return orderQuery;
    }

    /**
     * Get request data support for sorting.
     * @param jiraIssuesManager is used to parse filter to JQL
     * @param jiraIssuesColumnManager is used to get JIRA column info.
     * @param i18nBean is used to get localization data
     * @param parameters of jira issue macro
     * @param requestData for Jira issue macro
     * @param requestType is JQL or URL
     * @param conversionContext JIM context
     * @param applink ApplicationLink
     * @param jiraColumns all jira columns retrieve from REST API (/rest/api/2/field)
     * @return request data after added infomation for sorting
     * @throws MacroExecutionException if have any error otherwise
     */
    public static String getRequestDataForSorting(JiraIssuesManager jiraIssuesManager, JiraIssuesColumnManager jiraIssuesColumnManager, I18NBean i18nBean, Map<String, String> parameters, String requestData, Type requestType,
            ConversionContext conversionContext, ApplicationLink applink, Map<String, JiraColumnInfo> jiraColumns) throws MacroExecutionException
    {
        String orderColumnName = (String) conversionContext.getProperty("orderColumnName");
        String order = (String) conversionContext.getProperty("order");
        // Disable caching Jira issue.
        parameters.put("cache", "off");
        if (StringUtils.isBlank(orderColumnName))
        {
            return requestData;
        }
        List<JiraColumnInfo> columns = getColumnInfo(jiraIssuesColumnManager, i18nBean, parameters, jiraColumns);
        String columnKey = "";
        for (JiraColumnInfo columnInfo : columns)
        {
            if (columnInfo.getTitle().equalsIgnoreCase(orderColumnName))
            {
                columnKey = jiraIssuesColumnManager.getColumnMapping(columnInfo.getKey());
                break;
            }
        }
        String maximumIssuesStr = StringUtils.defaultString(parameters.get("maximumIssues"), String.valueOf(DEFAULT_NUMBER_OF_ISSUES));
        int maximumIssues = Integer.parseInt(maximumIssuesStr);
        if (maximumIssues > MAXIMUM_ISSUES)
        {
            maximumIssues = MAXIMUM_ISSUES;
        }
        return processJql(jiraIssuesManager, i18nBean, requestData, orderColumnName, columnKey, order, maximumIssues, requestType, applink);
    }

    public static List<String> getColumnNames(String columnsParameter)
    {
        List<String> columnNames = DEFAULT_RSS_FIELDS;

        if (StringUtils.isNotBlank(columnsParameter))
        {
            columnNames = new ArrayList<String>();
            List<String> keys = Arrays.asList(StringUtils.split(columnsParameter, ",;"));
            for (String key : keys)
            {
                if (StringUtils.isNotBlank(key))
                {
                    columnNames.add(key);
                }
            }

            if (columnNames.isEmpty())
            {
                columnNames = DEFAULT_RSS_FIELDS;
            }
        }
        return columnNames;
    }

    public static List<JiraColumnInfo> getColumnInfo(JiraIssuesColumnManager jiraIssuesColumnManager, I18NBean i18nBean, Map<String, String> params, Map<String, JiraColumnInfo> columns)
    {
        List<String> columnNames = getColumnNames(JiraUtil.getParamValue(params,"columns", PARAM_POSITION_1));
        List<JiraColumnInfo> info = new ArrayList<JiraColumnInfo>();
        for (String columnName : columnNames)
        {
            String key = jiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName);

            String i18nKey = PROP_KEY_PREFIX + key;
            String displayName = i18nBean.getText(i18nKey);

            // getText() unexpectedly returns the i18nkey if a value isn't found
            if (StringUtils.isBlank(displayName) || displayName.equals(i18nKey))
            {
                displayName = columnName;
            }

            if (columns.containsKey(key))
            {
                info.add(new JiraColumnInfo(key, displayName, StringUtils.isNotBlank(columns.get(key).getClauseName())));
            }
            else
            {
                info.add(new JiraColumnInfo(key, displayName, !JiraIssuesColumnManager.UNSUPPORT_SORTABLE_COLUMN_NAMES.contains(key)));
            }
        }

        return info;
    }

    private static String processJql(JiraIssuesManager jiraIssuesManager, I18NBean i18nBean, String requestData, String orderColumnName, String columnKey, String order, int maximumIssues, Type requestType, ApplicationLink applink) throws MacroExecutionException
    {
        StringBuilder retVal = new StringBuilder();
        if (requestType == Type.URL)
        {
            String jql = "";
            if (JiraJqlHelper.isFilterType(requestData))
            {
                jql = JiraJqlHelper.getJQLFromFilter(applink, requestData, jiraIssuesManager, i18nBean);
            }
            if (StringUtils.isNotBlank(jql))
            {
                StringBuffer sf = new StringBuffer(normalizeUrl(applink.getRpcUrl()));
                sf.append(XML_SEARCH_REQUEST_URI).append("?jqlQuery=");
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
                if (orderMatch.find())
                {
                    String orderColumns = jql.substring(orderMatch.end() - 1, jql.length());
                    jql = jql.substring(0, orderMatch.end() - 1);
                    // check orderColumn is exist on jql or not.
                    // first check column key
                    orderColumns = JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumnName, orderColumns);
                    retVal.append(url + JiraUtil.utf8Encode(jql + orderColumns) + "&tempMax=" + tempMax);
                }
                else // JQL does not have order by clause.
                {
                    requestData = " ORDER BY " + " \"" + columnKey + "\" " + order;
                    retVal.append(url + JiraUtil.utf8Encode(jql + requestData) + "&tempMax=" + tempMax);
                }
            }
        }
        else if (requestType == Type.JQL)
        {
            Matcher matcher = JiraJqlHelper.SORTING_PATTERN.matcher(requestData);
            if (matcher.find())
            {
                String orderColumns = requestData.substring(matcher.end() - 1, requestData.length());
                // check orderColumn is exist on jql or not.
                // first check column key
                orderColumns = JiraIssueSortableHelper.reoderColumns(order, columnKey, orderColumnName, orderColumns);
                retVal.append(requestData.substring(0, matcher.end() - 1) + orderColumns);
            }
            else // JQL does not have order by clause.
            {
                requestData = requestData + " ORDER BY " + " \"" + columnKey + "\" " + order;
                retVal.append(requestData);
            }
        }
        return retVal.toString();
    }

    private static String normalizeUrl(URI rpcUrl)
    {
        String baseUrl = rpcUrl.toString();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
