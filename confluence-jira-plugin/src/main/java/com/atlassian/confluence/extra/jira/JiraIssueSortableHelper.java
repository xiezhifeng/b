package com.atlassian.confluence.extra.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class JiraIssueSortableHelper {

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
        if (orderColumns.toLowerCase().contains(columnKey))
        {
            existColumn = columnKey;
        } 
        else if (orderColumns.toLowerCase().contains(columnName.toLowerCase()))
        {
            existColumn = columnName;
        }
        return existColumn;
    }

    /**
     * Reorder columns for sorting.
     * @param order can be "ASC" or "DESC"
     * @param columnKey for sorting
     * @param existColumn in orderColumns
     * @param orderColumns in JQL
     * @return new order columns in JQL
     */
    public static String reoderColumns(String order, String columnKey, String existColumn, String orderColumns)
    {
        
        if (StringUtils.isBlank(existColumn))
        {
            // order column does not exist. Should put order column with the highest priority.
            // EX: order column is key with asc in order. And jql= project = conf order by summary asc.
            // Then jql should be jql= project = conf order by key acs, summaryasc.
            return " \"" + columnKey + "\" " + (StringUtils.isBlank(order) ? "ASC " : order) + (StringUtils.isNotBlank(orderColumns) ? "," + orderColumns : "");
        }
        // calculate position column is exist.
        List<String> columnsIndex = Arrays.asList(orderColumns.split(","));
        int size = columnsIndex.size();
        if (size == 1)
        {
            return " \"" + columnKey + "\" " + order;
        }
        if (size > 1)
        {
            for (int i = 0; i < size; i++)
            {
                if (existColumn.equalsIgnoreCase(columnsIndex.get(i)))
                {
                    List<String> result = new ArrayList<String>();
                    String colData = columnsIndex.get(i);
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
                    for (String col : columnsIndex)
                    {
                        if (!col.equalsIgnoreCase(colData))
                        {
                            result.add(col);
                        }
                    }
                    orderColumns = StringUtils.join(result, ",");
                    break;
                }
            }
        }
        return orderColumns;
    }
}
