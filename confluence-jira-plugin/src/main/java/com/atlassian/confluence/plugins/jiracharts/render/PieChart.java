package com.atlassian.confluence.plugins.jiracharts.render;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_JQL;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_WIDTH;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.addJiraChartParameter;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.getCommonChartContext;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.getCommonJiraGadgetUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.JiraChartStatTypeManager;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.StatTypeModel;
import com.atlassian.confluence.web.UrlBuilder;

public class PieChart extends JiraImageChart
{
    private final static Logger LOGGER = LoggerFactory.getLogger(PieChart.class);
    private static final String PARAM_STAT_TYPE = "statType";

    private static final String[] chartParameters = new String[]{PARAM_STAT_TYPE};
    private ApplicationLinkService appLinkService;
    private JiraChartStatTypeManager jiraChartStatTypeManager;

    public PieChart(final ContextPathHolder pathHolder, final Base64JiraChartImageService base64JiraChartImageService, final JiraChartStatTypeManager jiraChartStatTypeManager, final ApplicationLinkService appLinkService)
    {
        this.base64JiraChartImageService = base64JiraChartImageService;
        this.pathHolder = pathHolder;
        this.jiraChartStatTypeManager = jiraChartStatTypeManager;
        this.appLinkService = appLinkService;
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
        contextMap.put(PARAM_STAT_TYPE, getStatLabel(parameters));
        contextMap.put("srcImg", getImageSource(context.getOutputType(), parameters, !result.isOAuthNeeded()));
        return contextMap;
    }

    private String getStatLabel(Map<String, String> parameters)
    {
        String statType = parameters.get(PARAM_STAT_TYPE);
        try {
            ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(parameters.get("serverId")));
            List<StatTypeModel> statTypes = jiraChartStatTypeManager.getStatTypes(appLink);
            for (StatTypeModel stat : statTypes)
            {
                if (stat.getValue().equalsIgnoreCase(statType))
                {
                    return stat.getLabel();
                }
            }
        } catch (TypeNotInstalledException e)
        {
            LOGGER.error("Can not find stats label cause by application link does not exist");
        }
        return statType;
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
