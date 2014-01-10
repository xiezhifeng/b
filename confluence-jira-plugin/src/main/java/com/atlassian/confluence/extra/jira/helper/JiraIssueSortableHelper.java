package com.atlassian.confluence.extra.jira.helper;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.atlassian.confluence.plugins.jira.JiraServerBean;

public class JiraIssueSortableHelper
{

    public static final long SUPPORT_JIRA_BUILD_NUMBER = 7000L; // should be change to build number of JIRA when it takes the fix on REST API to support sorting into account.
    public static final String SPACE = " ";
    public static final String DOUBLE_QUOTE = "\"";
    
    private static final List<String> DEFAULT_RSS_FIELDS = Arrays.asList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due");
    private static final String ASC = "ASC";
    private static final String DESC = "DESC";

    private JiraIssueSortableHelper()
    {
        
    }

    /**
     * Check if columnName or Column Key is exist in orderLolumns.
     * @param columnName will be checked
     * @param clauseName will be checked
     * @param orderColumns in JQL
     * @return column exists on order in jQL
     */
    private static String checkOrderColumnExistJQL(String clauseName, String orderColumns)
    {
        return orderColumns.trim().toLowerCase().contains(clauseName) ? clauseName : StringUtils.EMPTY; 
    }

    /**
     * Reorder columns for sorting.
     * @param order can be "ASC" or "DESC"
     * @param clauseName for sorting
     * @param orderQuery in JQL
     * @return new order columns in JQL
     */
    public static String reoderColumns(String order, String clauseName, String orderQuery)
    {
        String existColumn = JiraIssueSortableHelper.checkOrderColumnExistJQL(clauseName, orderQuery);
        if (StringUtils.isBlank(existColumn))
        {
            // order column does not exist. Should put order column with the highest priority.
            // EX: order column is key with asc in order. And jql= project = conf order by summary asc.
            // Then jql should be jql= project = conf order by key acs, summaryasc.
            return clauseName + SPACE + (StringUtils.isBlank(order) ? ASC : order) + (StringUtils.isNotBlank(orderQuery) ? "," + orderQuery : StringUtils.EMPTY);
        }
        // calculate position column is exist.
        List<String> orderQueries = Arrays.asList(orderQuery.split(","));
        int size = orderQueries.size();
        if (size == 1)
        {
            return clauseName + SPACE + order;
        }
        if (size > 1)
        {
            for (int i = 0; i < size; i++)
            { // order by key desc, summary asc
                if (orderQueries.get(i).contains(existColumn))
                {
                    List<String> result = new ArrayList<String>();
                    String colData = orderQueries.get(i);
                    if (colData.toUpperCase().contains(ASC))
                    {
                        result.add(colData.toUpperCase().replace(ASC, order));
                    }
                    else if (colData.toUpperCase().contains(DESC))
                    {
                        result.add(colData.toUpperCase().replace(ASC, order));
                    }
                    else
                    {
                        result.add( colData + SPACE + order);
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

    public static String normalizeUrl(URI rpcUrl)
    {
        String baseUrl = rpcUrl.toString();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static boolean isJiraSupportedOrder(JiraServerBean jiraServer)
    {
        return jiraServer.getBuildNumber() >= JiraIssueSortableHelper.SUPPORT_JIRA_BUILD_NUMBER;
    }
}

