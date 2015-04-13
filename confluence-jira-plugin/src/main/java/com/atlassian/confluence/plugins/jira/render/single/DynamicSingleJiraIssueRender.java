package com.atlassian.confluence.plugins.jira.render.single;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.util.velocity.VelocityUtils;

import java.util.List;
import java.util.Map;

public class DynamicSingleJiraIssueRender extends SingleJiraIssueRender
{

    @Override
    public void populateSpecifyMacroType(Map<String, Object> contextMap, List<String> columnNames, String url, ApplicationLink appLink, boolean forceAnonymous,
                                         boolean useCache, ConversionContext conversionContext, JiraRequestData jiraRequestData, Map<String, String> params) throws MacroExecutionException
    {
        contextMap.put("applink", appLink);
        setKeyInContextMap(jiraRequestData, contextMap);
    }

    @Override
    public String getTemplate(Map<String, Object> contextMap) {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/dynamicJiraIssues.vm", contextMap);
    }
}
