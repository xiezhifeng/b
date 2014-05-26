package com.atlassian.confluence.plugins.jiracharts.render;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_JQL;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_WIDTH;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.addJiraChartParameter;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.getCommonChartContext;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.getCommonJiraGadgetUrl;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.web.UrlBuilder;

public class PieChart extends JiraImageChart
{
    private static final String PARAM_STAT_TYPE = "statType";

    private static final String[] chartParameters = new String[]{PARAM_STAT_TYPE};

    public PieChart(final ContextPathHolder pathHolder, final Base64JiraChartImageService base64JiraChartImageService)
    {
        this.base64JiraChartImageService = base64JiraChartImageService;
        this.pathHolder = pathHolder;
    }

    @Override
    public String[] getChartParameters()
    {
        return chartParameters;
    }

    @Override
    public String getTemplateFileName()
    {
        return "piechart.vm";
    }

    @Override
    public String getJiraGadgetRestUrl()
    {
        return "/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-";
    }

    @Override
    public String getJiraGadgetUrl(HttpServletRequest request)
    {
        UrlBuilder urlBuilder = getCommonJiraGadgetUrl(request.getParameter(PARAM_JQL), request.getParameter(PARAM_WIDTH), getJiraGadgetRestUrl());
        addJiraChartParameter(urlBuilder, request, getChartParameters());
        return urlBuilder.toString();
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {

        Map<String, Object> contextMap = getCommonChartContext(parameters, result, context);
        String statType = parameters.get(PARAM_STAT_TYPE);
        contextMap.put(PARAM_STAT_TYPE, statType);
        contextMap.put("srcImg", getImageSource(context.getOutputType(), parameters, !result.isOAuthNeeded()));
        return contextMap;
    }

    @Override
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        return urlBuilder.toString();
    }

    @Override
    public String getDefaultPDFChartWidth()
    {
        return "320";
    }
}
