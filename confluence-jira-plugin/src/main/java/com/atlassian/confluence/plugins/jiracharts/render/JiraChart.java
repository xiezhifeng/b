package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.web.UrlBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Jira chart interface
 * @since 5.5.3
 */
public interface JiraChart
{

    /**
     * setup context to render chart for all jira chart
     * @param parameters parameters of jira chart macro
     * @param result JQLValidationResult
     * @param context ConversionContext
     * @return context map for view page
     */
    Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException;

    /**
     * get image place holder for chart
     * @param parameters chart parameters
     * @param urlBuilder url builder
     * @return image url
     */
    String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder);

    /**
     *
     * @return rest gadget url of chart
     */
    String getJiraGadgetRestUrl();

    /**
     * get gadget url base on request params
     * @param request http request
     * @return gadget url
     */
    String getJiraGadgetUrl(HttpServletRequest request);

    /**
     * template file for each chart
     * @return template file
     */
    String getTemplateFileName();

    /**
     * get chart parameters
     * @return chart parameters
     */
    String[] getChartParameters();
}
