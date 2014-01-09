package com.atlassian.confluence.extra.jira;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    public static long SUPPORT_JIRA_BUILD_NUMBER = 7000L; // should be change to build number of JIRA when it takes the fix on REST API to support sorting into account.

    private static final int PARAM_POSITION_1 = 1;
    private static final int DEFAULT_NUMBER_OF_ISSUES = 20;
    private static final int MAXIMUM_ISSUES = 1000;
    private static final List<String> DEFAULT_RSS_FIELDS = Arrays.asList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due");
    private static final String PROP_KEY_PREFIX = "jiraissues.column.";
    private static final String XML_SEARCH_REQUEST_URI = "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";
    
    private static final Map<String, String> columnkeysMapping;
    static
    {
        columnkeysMapping = new HashMap<String, String>();
        columnkeysMapping.put("version", "affectedVersion");
        columnkeysMapping.put("security", "level");
        columnkeysMapping.put("watches", "watchers");
    }

    private JiraIssueSortableHelper()
    {
        
    }

    /**
     * Get columnKey is mapped between JIRA and JIM to support sortable ability.
     * @param columnKey is key from JIM
     * @return key has mapped.
     */
    private static String getColumnMapping(String columnKey)
    {
        String key = columnkeysMapping.get(columnKey);
        if (StringUtils.isNotBlank(key))
        {
            return key;
        }
        return columnKey;
    }
    /**
     * Check if columnName or Column Key is exist in orderLolumns.
     * @param columnName will be checked
     * @param columnKey will be checked
     * @param orderColumns in JQL
     * @return column exists on order in jQL
     */
    public static String checkOrderColumnExistJQL(String columnName, String clauseName, String orderColumns)
    {
        String existColumn = "";
        if (orderColumns.trim().toLowerCase().contains(clauseName))
        {
            existColumn = clauseName;
        } 
        else if (orderColumns.trim().toLowerCase().contains(columnName.toLowerCase()))
        {
            existColumn = columnName;
        }
        return existColumn;
    }

    /**
     * Reorder columns for sorting.
     * @param order can be "ASC" or "DESC"
     * @param clauseName for sorting
     * @param existColumn in orderColumns
     * @param orderQuery in JQL
     * @return new order columns in JQL
     */
    public static String reoderColumns(String order, String clauseName, String orderColumnName, String orderQuery)
    {
        String existColumn = JiraIssueSortableHelper.checkOrderColumnExistJQL(orderColumnName, clauseName, orderQuery);
        if (StringUtils.isBlank(existColumn))
        {
            // order column does not exist. Should put order column with the highest priority.
            // EX: order column is key with asc in order. And jql= project = conf order by summary asc.
            // Then jql should be jql= project = conf order by key acs, summaryasc.
            return " \"" + clauseName + "\" " + (StringUtils.isBlank(order) ? "ASC " : order) + (StringUtils.isNotBlank(orderQuery) ? "," + orderQuery : "");
        }
        // calculate position column is exist.
        List<String> orderQueries = Arrays.asList(orderQuery.split(","));
        int size = orderQueries.size();
        if (size == 1)
        {
            return " \"" + clauseName + "\" " + order;
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
        String clauseName = getClauseName(jiraIssuesColumnManager, i18nBean, parameters, jiraColumns, orderColumnName);
        String maximumIssuesStr = StringUtils.defaultString(parameters.get("maximumIssues"), String.valueOf(DEFAULT_NUMBER_OF_ISSUES));
        int maximumIssues = Integer.parseInt(maximumIssuesStr);
        if (maximumIssues > MAXIMUM_ISSUES)
        {
            maximumIssues = MAXIMUM_ISSUES;
        }
        switch (requestType)
        {
            case URL:
                return getSortDataFromUrl(jiraIssuesManager, i18nBean, requestData, orderColumnName, clauseName, order, maximumIssues, applink);
            case JQL:
                return getSortDataFromJQL(requestData, orderColumnName, clauseName, order);
            default:
                return requestData;
        }
    }

    private static String getClauseName(JiraIssuesColumnManager jiraIssuesColumnManager, I18NBean i18nBean, Map<String, String> parameters, Map<String, JiraColumnInfo> jiraColumns, String orderColumnName)
    {
        List<JiraColumnInfo> columns = getColumnInfo(jiraIssuesColumnManager, i18nBean, parameters, jiraColumns);
        String clauseName = StringUtils.EMPTY;
        for (JiraColumnInfo columnInfo : columns)
        {
            if (columnInfo.getTitle().equalsIgnoreCase(orderColumnName))
            {
                clauseName = getColumnMapping(columnInfo.getClauseName().get(0));
                break;
            }
        }
        return clauseName;
    }
    /**
     * Gets column names base on column parameter from JIM.
     * @param columnsParameter columns parameter frim JIM
     * @return a list of column names
     */
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

    /**
     *  Gets column info from JIRA and provides sorting ability.
     * @param jiraIssuesColumnManager manages JIRA issue columns
     * @param i18nBean localization 
     * @param params JIRA issue macro parameters
     * @param columns retrieve from REST API 
     * @return jira column info
     */
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
                info.add(new JiraColumnInfo(key, displayName, columns.get(key).getClauseName(),!columns.get(key).getClauseName().isEmpty()));
            }
            else
            {
                // at this point clause name is column key.
                info.add(new JiraColumnInfo(key, displayName, Arrays.asList(key), !JiraIssuesColumnManager.UNSUPPORT_SORTABLE_COLUMN_NAMES.contains(key)));
            }
        }

        return info;
    }

    private static String getSortDataFromUrl(JiraIssuesManager jiraIssuesManager, I18NBean i18nBean, String requestData, String orderColumnName, String clauseName, String order, int maximumIssues, ApplicationLink applink) throws MacroExecutionException
    {
        StringBuilder retVal = new StringBuilder();
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
                orderColumns = JiraIssueSortableHelper.reoderColumns(order, clauseName, orderColumnName, orderColumns);
                retVal.append(url + JiraUtil.utf8Encode(jql + orderColumns) + "&tempMax=" + tempMax);
            }
            else // JQL does not have order by clause.
            {
                requestData = " ORDER BY " + " \"" + clauseName + "\" " + order;
                retVal.append(url + JiraUtil.utf8Encode(jql + requestData) + "&tempMax=" + tempMax);
            }
        }
        return retVal.toString();
    }

    private static String getSortDataFromJQL(String requestData, String orderColumnName, String columnKey, String order) throws MacroExecutionException
    {
        StringBuilder retVal = new StringBuilder();
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
        return retVal.toString();
    }

    private static String normalizeUrl(URI rpcUrl)
    {
        String baseUrl = rpcUrl.toString();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
