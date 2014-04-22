package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_JQL;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_SERVER_ID;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_WIDTH;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.addJiraChartParameter;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.getCommonJiraGadgetUrl;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.getCommonServletJiraChartUrl;

public abstract class JiraImageChart implements JiraChart
{
    private static Logger log = LoggerFactory.getLogger(JiraImageChart.class);

    protected ContextPathHolder pathHolder;

    protected Base64JiraChartImageService base64JiraChartImageService;

    /**
     * get gadget url base on request params
     * @param request http request
     * @return gadget url
     */
    public abstract String getJiraGadgetUrl(HttpServletRequest request);

    public abstract String getDefaultPDFChartWidth();


    protected String getImageSource(String outputType, Map<String, String> parameters, boolean isAuthenticated) throws MacroExecutionException
    {
        if (RenderContextOutputType.PDF.equals(outputType))
        {
            try
            {
                String width = StringUtils.isBlank(parameters.get(PARAM_WIDTH)) ? getDefaultPDFChartWidth() : parameters.get(PARAM_WIDTH);
                UrlBuilder urlBuilder = getCommonJiraGadgetUrl(parameters.get(PARAM_JQL), width, getJiraGadgetRestUrl());
                addJiraChartParameter(urlBuilder, parameters, getChartParameters());
                return base64JiraChartImageService.getBase64JiraChartImage(parameters.get(PARAM_SERVER_ID), urlBuilder.toString());
            }
            catch (ResponseException e)
            {
                log.debug("Can not retrieve jira chart image for export pdf");
                throw new MacroExecutionException(e);
            }
        }
        else
        {
            UrlBuilder urlBuilder = getCommonServletJiraChartUrl(parameters, pathHolder.getContextPath(), isAuthenticated);
            addJiraChartParameter(urlBuilder, parameters, getChartParameters());
            return urlBuilder.toString();
        }
    }

}
