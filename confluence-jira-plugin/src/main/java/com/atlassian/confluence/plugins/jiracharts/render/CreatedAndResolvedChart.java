package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.JiraImageChartModel;
import com.atlassian.confluence.web.UrlBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.*;

public class CreatedAndResolvedChart extends JiraImageChart
{
    private static final String CHART_WIDTH_DEFAULT = "390";

    private static final List<String> chartParameters = Collections.unmodifiableList(Arrays.asList("periodName", "daysprevious", "isCumulative", "showUnresolvedTrend", "versionLabel"));

    private static final String DEFAULT_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jirachart_images/jirachart_placeholder.png";

    public CreatedAndResolvedChart(final ContextPathHolder pathHolder, final Base64JiraChartImageService base64JiraChartImageService)
    {

        this.base64JiraChartImageService = base64JiraChartImageService;
        this.pathHolder = pathHolder;
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = getCommonChartContext(parameters, result, context);
        contextMap.put("daysprevious", parameters.get("daysprevious"));
        contextMap.put("periodName", parameters.get("periodName"));
        JiraImageChartModel chartModel = getImageSourceModel(parameters, context.getOutputType());
        contextMap.put("srcImg", chartModel.getBase64Image());
        contextMap.put("issuesCreated", chartModel.getIssuesCreated());
        contextMap.put("issuesResolved", chartModel.getIssuesResolved());

        return contextMap;
    }

    @Override
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        urlBuilder.add(PARAM_WIDTH, CHART_WIDTH_DEFAULT);
        addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        return urlBuilder.toString();
    }

    @Override
    public String getDefaultImagePlaceholderUrl()
    {
        return DEFAULT_PLACEHOLDER_IMG_PATH;
    }

    @Override
    public String getJiraGadgetRestUrl()
    {
        return "/rest/gadget/1.0/createdVsResolved/generate?projectOrFilterId=jql-";
    }

    @Override
    public String getJiraGadgetUrl(HttpServletRequest request)
    {
        UrlBuilder urlBuilder = getCommonJiraGadgetUrl(request.getParameter(PARAM_JQL), request.getParameter(PARAM_WIDTH), getJiraGadgetRestUrl());
        addJiraChartParameter(urlBuilder, request, getChartParameters());
        return urlBuilder.toString();
    }

    @Override
    public String getTemplateFileName()
    {
        return "created-vs-resolved-chart.vm";
    }

    @Override
    public String[] getChartParameters()
    {
        return chartParameters.toArray(new String[chartParameters.size()]);
    }

    @Override
    public String getDefaultPDFChartWidth()
    {
        return CHART_WIDTH_DEFAULT;
    }
}
