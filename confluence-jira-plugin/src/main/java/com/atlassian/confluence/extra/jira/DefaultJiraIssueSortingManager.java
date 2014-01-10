package com.atlassian.confluence.extra.jira;

import java.util.ArrayList;
import java.util.Arrays;
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
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;

public class DefaultJiraIssueSortingManager implements JiraIssueSortingManager
{
    private static final String PROP_KEY_PREFIX = "jiraissues.column.";
    
    private JiraIssuesColumnManager jiraIssuesColumnManager;
    private JiraIssuesManager jiraIssuesManager;
    private I18NBeanFactory i18NBeanFactory;
    private LocaleManager localeManager;

    private I18NBean getI18NBean()
    {
        if (null != AuthenticatedUserThreadLocal.get())
        {
            return i18NBeanFactory.getI18NBean(localeManager.getLocale(AuthenticatedUserThreadLocal.get()));
        }
        return i18NBeanFactory.getI18NBean();
    }
    public DefaultJiraIssueSortingManager(JiraIssuesColumnManager jiraIssuesColumnManager, JiraIssuesManager jiraIssuesManager, I18NBeanFactory i18nBeanFactory, LocaleManager localeManager)
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.jiraIssuesManager =jiraIssuesManager;
        this.i18NBeanFactory = i18nBeanFactory;
        this.localeManager = localeManager;
    }
    @Override
    public List<JiraColumnInfo> getColumnInfo(Map<String, String> params, Map<String, JiraColumnInfo> columns)
    {
        List<String> columnNames = JiraIssueSortableHelper.getColumnNames(JiraUtil.getParamValue(params,"columns", JiraUtil.PARAM_POSITION_1));
        List<JiraColumnInfo> info = new ArrayList<JiraColumnInfo>();
        for (String columnName : columnNames)
        {
            String key = jiraIssuesColumnManager.getCanonicalFormOfBuiltInField(columnName);

            String i18nKey = PROP_KEY_PREFIX + key;
            String displayName = getI18NBean().getText(i18nKey);

            // getText() unexpectedly returns the i18nkey if a value isn't found
            if (StringUtils.isBlank(displayName) || displayName.equals(i18nKey))
            {
                displayName = columnName;
            }
            
            if (null != columns && columns.containsKey(key))
            {
                info.add(new JiraColumnInfo(key, displayName, columns.get(key).getClauseName() != null ? columns.get(key).getClauseName() : Arrays.asList(key) ,columns.get(key).getClauseName() != null ? Boolean.TRUE : Boolean.FALSE));
            }
            else
            {
                // at this point clause name is column key.
                info.add(new JiraColumnInfo(key, displayName, Arrays.asList(key), !JiraIssuesColumnManager.UNSUPPORT_SORTABLE_COLUMN_NAMES.contains(key)));
            }
        }

        return info;
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
                return getUrlSortRequest(requestData, orderColumnName, clauseName, order, JiraUtil.getMaximumIssues(parameters), applink);
            case JQL:
                return getJQLSortRequest(requestData, orderColumnName, clauseName, order);
            default:
                return requestData;
        }
    }

    private String getClauseName(Map<String, String> parameters, Map<String, JiraColumnInfo> jiraColumns, String orderColumnName)
    {
        List<JiraColumnInfo> columns = getColumnInfo(parameters, jiraColumns);
        for (JiraColumnInfo columnInfo : columns)
        {
            if (columnInfo.getTitle().equalsIgnoreCase(orderColumnName))
            {
                return JiraIssueSortableHelper.getColumnMapping(columnInfo.getClauseName().get(0));
            }
        }
        return StringUtils.EMPTY;
    }
    
    private String getUrlSortRequest(String requestData, String orderColumnName, String clauseName, String order, int maximumIssues, ApplicationLink applink) throws MacroExecutionException
    {
        StringBuilder urlSort = new StringBuilder();
        String jql = StringUtils.EMPTY;
        if (JiraJqlHelper.isFilterType(requestData))
        {
            jql = JiraJqlHelper.getJQLFromFilter(applink, requestData, jiraIssuesManager, getI18NBean());
        }
        if (StringUtils.isNotBlank(jql))
        {
            StringBuffer sf = new StringBuffer(JiraIssueSortableHelper.normalizeUrl(applink.getRpcUrl()));
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
                orderColumns = JiraIssueSortableHelper.reoderColumns(order, clauseName, orderColumnName, orderColumns);
                orderData = orderColumns;
            }
            else // JQL does not have order by clause.
            {
                orderData = " ORDER BY " + clauseName + JiraIssueSortableHelper.SPACE + order;
            }
            urlSort.append(url + JiraUtil.utf8Encode(jql + orderData) + "&tempMax=" + tempMax);
        }
        return urlSort.toString();
    }

    private String getJQLSortRequest(String requestData, String orderColumnName, String clauseName, String order) throws MacroExecutionException
    {
        StringBuilder urlSort = new StringBuilder();
        Matcher matcher = JiraJqlHelper.SORTING_PATTERN.matcher(requestData);
        if (matcher.find())
        {
            String orderColumns = requestData.substring(matcher.end() - 1, requestData.length());
            // check orderColumn is exist on jql or not.
            // first check column key
            orderColumns = JiraIssueSortableHelper.reoderColumns(order, clauseName, orderColumnName, orderColumns);
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
