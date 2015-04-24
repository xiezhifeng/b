package com.atlassian.confluence.plugins.jira.render.single;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.util.velocity.VelocityUtils;

import java.util.Map;

public class DynamicSingleJiraIssueRender extends SingleJiraIssueRender
{

    @Override
    public void populateSpecifyMacroType(Map<String, Object> contextMap, ApplicationLink appLink, ConversionContext conversionContext, JiraRequestData jiraRequestData) throws MacroExecutionException
    {
        contextMap.put("applink", appLink);
    }

    @Override
    public String getTemplate(Map<String, Object> contextMap, boolean isMobileMode) {
        return VelocityUtils.getRenderedTemplate(isMobileMode ? TEMPLATE_MOBILE_PATH + "/mobileSingleJiraIssue.vm" : TEMPLATE_PATH + "/dynamicJiraIssues.vm", contextMap);
    }
}
