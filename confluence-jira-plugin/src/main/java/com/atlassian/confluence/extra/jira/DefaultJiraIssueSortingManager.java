package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
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
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class DefaultJiraIssueSortingManager implements JiraIssueSortingManager
{
    private final JiraIssuesColumnManager jiraIssuesColumnManager;
    private final JiraIssuesManager jiraIssuesManager;
    private final I18NBeanFactory i18nBeanFactory;
    private final LocaleManager localeManager;

    public DefaultJiraIssueSortingManager(JiraIssuesColumnManager jiraIssuesColumnManager, JiraIssuesManager jiraIssuesManager, LocaleManager localeManager, I18NBeanFactory i18nBeanFactory)
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.jiraIssuesManager = jiraIssuesManager;
        this.localeManager = localeManager;
        this.i18nBeanFactory = i18nBeanFactory;
    }

    public I18NBean getI18NBean()
    {
        if (null != AuthenticatedUserThreadLocal.get())
        {
            return i18nBeanFactory.getI18NBean(localeManager.getLocale(AuthenticatedUserThreadLocal.get()));
        }
        return i18nBeanFactory.getI18NBean();
    }

    @Override
    public String getRequestDataForSorting(Map<String, String> parameters, String requestData, Type requestType, Map<String, JiraColumnInfo> jiraColumns, ConversionContext conversionContext, ReadOnlyApplicationLink applink) throws MacroExecutionException
    {
        String orderColumnName = (String) conversionContext.getProperty("orderColumnName");
        String order = (String) conversionContext.getProperty("order");
        
        if (StringUtils.isBlank(orderColumnName))
        {
            return requestData;
        }
        // Disable caching Jira issue.
        parameters.put("cache", "off");
        String clauseName = getClauseName(parameters, jiraColumns, orderColumnName, applink);
        switch (requestType)
        {
            case URL:
                return getUrlSortRequest(requestData, clauseName, order, jiraColumns, JiraUtil.getMaximumIssues(parameters.get("maximumIssues")), applink);
            case JQL:
                return getJQLSortRequest(requestData, clauseName, order, jiraColumns);
            default:
                return requestData;
        }
    }

    private String getClauseName(final Map<String, String> parameters, final Map<String, JiraColumnInfo> jiraColumns, final String orderColumnName, final ReadOnlyApplicationLink applink)
    {
        List<JiraColumnInfo> columns = jiraIssuesColumnManager.getColumnInfo(parameters, jiraColumns, applink);
        for (JiraColumnInfo columnInfo : columns)
        {
            if (columnInfo.getTitle().equalsIgnoreCase(orderColumnName))
            {
                return jiraIssuesColumnManager.getColumnMapping(columnInfo.getPrimaryClauseName(), JiraIssuesColumnManager.COLUMN_KEYS_MAPPING);
            }
        }
        return StringUtils.EMPTY;
    }

    private String getUrlSortRequest(String requestData, String clauseName, String order, Map<String, JiraColumnInfo> jiraColumns, int maximumIssues, ReadOnlyApplicationLink applink) throws MacroExecutionException
    {
        StringBuilder urlSort = new StringBuilder();
        String jql = StringUtils.EMPTY;
        if (JiraJqlHelper.isUrlFilterType(requestData))
        {
            jql = JiraJqlHelper.getJQLFromFilter(applink, requestData, jiraIssuesManager, getI18NBean());
        }

        if (StringUtils.isNotBlank(jql))
        {
            StringBuffer sf = new StringBuffer(JiraUtil.normalizeUrl(applink.getRpcUrl()));
            sf.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?jqlQuery=");
            sf.append(JiraUtil.utf8Encode(jql)).append("&tempMax=" + maximumIssues);
            requestData = sf.toString();
        }

        Matcher matcher = JiraJqlHelper.XML_SORTING_PATTERN.matcher(requestData);
        Matcher matcher2 = JiraJqlHelper.XML_SORTING_PATTERN_TEMPMAX.matcher(requestData);
        if (matcher.find() || matcher2.find())
        {
            String tempMax;
            String url;

            if(matcher.find())
            {
                jql = JiraUtil.utf8Decode(JiraJqlHelper.getValueByRegEx(requestData, JiraJqlHelper.XML_SORTING_PATTERN, 2));
                tempMax = JiraJqlHelper.getValueByRegEx(requestData, JiraJqlHelper.XML_SORTING_PATTERN, 3);
                url = requestData.substring(0, matcher.end(1) + 1);
            }
            else
            {
                jql = JiraUtil.utf8Decode(JiraJqlHelper.getValueByRegEx(requestData, JiraJqlHelper.XML_SORTING_PATTERN_TEMPMAX, 3));
                tempMax = JiraJqlHelper.getValueByRegEx(requestData, JiraJqlHelper.XML_SORTING_PATTERN_TEMPMAX, 2);
                url = requestData.substring(0, matcher2.end(1) + 1).replaceAll(JiraJqlHelper.TEMPMAX, "");
            }

            Matcher orderMatch = JiraJqlHelper.SORTING_PATTERN.matcher(jql);
            String orderData;
            if (orderMatch.find())
            {
                String orderColumns = jql.substring(orderMatch.end() - 1, jql.length());
                jql = jql.substring(0, orderMatch.end() - 1);
                // check orderColumn is exist on jql or not.
                
                // first check column key
                orderData = JiraIssueSortableHelper.reoderColumns(order, clauseName, orderColumns, jiraColumns);
            }
            else // JQL does not have order by clause.
            {
                orderData = " ORDER BY " + JiraIssueSortableHelper.DOUBLE_QUOTE + JiraUtil.escapeDoubleQuote(clauseName) + JiraIssueSortableHelper.DOUBLE_QUOTE + JiraIssueSortableHelper.SPACE + order;
            }
            urlSort.append(url + JiraUtil.utf8Encode(jql + orderData) + "&tempMax=" + tempMax);
        }
        return urlSort.toString();
    }

    private String getJQLSortRequest(String requestData, String clauseName, String order, Map<String, JiraColumnInfo> jiraColumns) throws MacroExecutionException
    {
        StringBuilder jqlSort = new StringBuilder();
        Matcher matcher = JiraJqlHelper.SORTING_PATTERN.matcher(requestData);
        if (matcher.find())
        {
            String orderColumns = requestData.substring(matcher.end() - 1, requestData.length());
            // check orderColumn is exist on jql or not.
            // first check column key
            orderColumns = JiraIssueSortableHelper.reoderColumns(order, clauseName, orderColumns, jiraColumns);
            jqlSort.append(requestData.substring(0, matcher.end() - 1) + orderColumns);
        }
        else // JQL does not have order by clause.
        {
            requestData = requestData + " ORDER BY " + JiraIssueSortableHelper.DOUBLE_QUOTE + JiraUtil.escapeDoubleQuote(clauseName) + JiraIssueSortableHelper.DOUBLE_QUOTE + JiraIssueSortableHelper.SPACE + order;
            jqlSort.append(requestData);
        }

        return jqlSort.toString();
    }
}
