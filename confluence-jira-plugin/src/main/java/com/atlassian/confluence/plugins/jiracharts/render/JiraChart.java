package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.web.UrlBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface JiraChart
{

    /**
     *
     * @param parameters parameters of jira chart macro
     * @param result JQLValidationResult
     * @param context ConversionContext
     * @return context map for view page
     */
    Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException;

    String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder);

    String getJiraGagetUrl();

    String getJiraGagetUrl(HttpServletRequest request);

    String getTemplateFileName();

    String[] getChartParameters();
}
