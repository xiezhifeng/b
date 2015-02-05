package com.atlassian.confluence.plugins.jira.render.table;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import org.apache.commons.lang.StringUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DynamicTableJiraIssueRender extends TableJiraIssueRender {

    @Override
    public void populateSpecifyMacroType(Map<String, Object> contextMap, List<String> columnNames, String url, ApplicationLink appLink, boolean forceAnonymous,
                                         boolean useCache, ConversionContext conversionContext, JiraRequestData jiraRequestData, Map<String, String> params) throws MacroExecutionException
    {
        if (appLink != null) {
            contextMap.put("applink", appLink);
        }

        StringBuffer urlBuffer = new StringBuffer(url);
        contextMap.put("resultsPerPage", getResultsPerPageParam(urlBuffer));

        // unfortunately this is ignored right now, because the javascript has not been made to handle this (which may require hacking and this should be a rare use-case)
        String startOn = getStartOnParam(params.get("startOn"), urlBuffer);
        contextMap.put("startOn",  new Integer(startOn));
        contextMap.put("sortOrder",  getSortOrderParam(urlBuffer));
        contextMap.put("sortField",  getSortFieldParam(urlBuffer));
        contextMap.put("useCache", useCache);

        // name must end in "Html" to avoid auto-encoding
        contextMap.put("retrieverUrlHtml", buildRetrieverUrl((List<JiraColumnInfo>)contextMap.get(JiraIssuesMacro.COLUMNS), urlBuffer.toString(), appLink, forceAnonymous));
    }

    private String getStartOnParam(String startOn, StringBuffer urlParam)
    {
        String pagerStart = filterOutParam(urlParam,"pager/start=");
        if (StringUtils.isNotEmpty(startOn))
        {
            return startOn.trim();
        }

        if (StringUtils.isNotEmpty(pagerStart))
        {
            return pagerStart;
        }
        return "0";
    }

    private String getSortOrderParam(StringBuffer urlBuffer)
    {
        String sortOrder = filterOutParam(urlBuffer, "sorter/order=");
        if (StringUtils.isNotEmpty(sortOrder))
        {
            return sortOrder.toLowerCase();
        }
        return "desc";
    }


    private String getSortFieldParam(StringBuffer urlBuffer)
    {
        String sortField = filterOutParam(urlBuffer, "sorter/field=");
        if (StringUtils.isNotEmpty(sortField))
        {
            return sortField;
        }
        return null;
    }

    private int getResultsPerPageParam(StringBuffer urlParam) throws MacroExecutionException
    {
        String tempMaxParam = filterOutParam(urlParam, "tempMax=");
        if (StringUtils.isNotEmpty(tempMaxParam))
        {
            int tempMax = Integer.parseInt(tempMaxParam);
            if (tempMax <= 0)
            {
                throw new MacroExecutionException("The tempMax parameter in the JIRA url must be greater than zero.");
            }
            return tempMax;
        }
        else
        {
            return 10;
        }
    }

    private String buildRetrieverUrl(Collection<JiraColumnInfo> columns, String url, ApplicationLink applicationLink, boolean forceAnonymous)
    {
        String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
        StringBuilder retrieverUrl = new StringBuilder(baseUrl);
        retrieverUrl.append("/plugins/servlet/issue-retriever?");
        retrieverUrl.append("url=").append(JiraUtil.utf8Encode(url));
        if (applicationLink != null)
        {
            retrieverUrl.append("&appId=").append(JiraUtil.utf8Encode(applicationLink.getId().toString()));
        }
        for (JiraColumnInfo columnInfo : columns)
        {
            retrieverUrl.append("&columns=").append(JiraUtil.utf8Encode(columnInfo.toString()));
        }
        retrieverUrl.append("&forceAnonymous=").append(forceAnonymous);
        retrieverUrl.append("&flexigrid=true");
        return retrieverUrl.toString();
    }

    @Override
    public String getTemplate(Map<String, Object> contextMap) {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/dynamicJiraIssues.vm", contextMap);
    }


    private SettingsManager settingsManager;


    public void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }
}
