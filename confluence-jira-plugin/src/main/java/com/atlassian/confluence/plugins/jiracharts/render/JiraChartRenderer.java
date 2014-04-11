package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface JiraChartRenderer
{

    /**
     *
     * @param parameters parameters of jira chart macro
     * @param result JQLValidationResult
     * @param context ConversionContext
     * @return context map for view page
     */
    Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException;

    ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext context);

    String getJiraGagetUrl();

    String getJiraGagetUrl(HttpServletRequest request);
}
