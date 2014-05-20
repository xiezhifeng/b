package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.definition.RichTextMacroBody;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.TwoDimensionalChartModel;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public class TwoDimensionalChart extends JiraHtmlChart
{

    private static final String[] chartParameters = new String[]{"xstattype", "ystattype", "showTotals", "sortDirection", "sortBy"};
    private static final String CHART_WIDTH_DEFAULT = "590";
    private static final String MAX_NUMBER_TO_SHOW_VALUE = "999";

    private MacroMarshallingFactory macroMarshallingFactory;

    public TwoDimensionalChart(ApplicationLinkService applicationLinkService, MacroMarshallingFactory macroMarshallingFactory)
    {
        this.applicationLinkService = applicationLinkService;
        this.macroMarshallingFactory = macroMarshallingFactory;
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = JiraChartHelper.getCommonChartContext(parameters, result, context);

        String jql = parameters.get(JiraChartHelper.PARAM_JQL);
        String width = parameters.get(JiraChartHelper.PARAM_WIDTH);
        if(StringUtils.isBlank(width))
        {
            width = CHART_WIDTH_DEFAULT;
        }

        String numberToShow = getNumberToShow(context, parameters.get("numberToShow"));

        UrlBuilder urlBuilder = JiraChartHelper.getCommonJiraGadgetUrl(jql, width, getJiraGadgetRestUrl());
        JiraChartHelper.addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        urlBuilder.add("numberToShow", numberToShow);

        TwoDimensionalChartModel chart = (TwoDimensionalChartModel) getChartModel(parameters.get(JiraChartHelper.PARAM_SERVER_ID), urlBuilder.toString());

        contextMap.put("chartModel", chart);
        contextMap.put(JiraChartHelper.PARAM_WIDTH, width + "px");
        contextMap.put("numberToShow", numberToShow.equals(MAX_NUMBER_TO_SHOW_VALUE) ? chart.getTotalRows() : numberToShow);

        if(isShowLink(context, numberToShow, chart.getTotalRows()))
        {
            setupShowLink(contextMap, parameters, context);
        }

        return contextMap;
    }

    @Override
    public Class<TwoDimensionalChartModel> getChartModelClass()
    {
        return TwoDimensionalChartModel.class;
    }

    @Override
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        return "/download/resources/confluence.extra.jira/jirachart_images/twodimensional-chart-placeholder.png";
    }

    @Override
    public String getJiraGadgetRestUrl()
    {
        return "/rest/gadget/1.0/twodimensionalfilterstats/generate?filterId=jql-";
    }

    @Override
    public String getTemplateFileName()
    {
        return "two-dimensional-chart.vm";
    }

    @Override
    public String[] getChartParameters()
    {
        return chartParameters;
    }


    private int getNextRefreshId()
    {
        return RandomUtils.nextInt();
    }

    private String getNumberToShow(ConversionContext context, String numberToShow)
    {
        if(context.hasProperty("isShowMore") && Boolean.valueOf(context.getPropertyAsString("isShowMore")))
        {
            return MAX_NUMBER_TO_SHOW_VALUE;
        }

        if(StringUtils.isBlank(numberToShow))
        {
            return MAX_NUMBER_TO_SHOW_VALUE;
        }

        return numberToShow;
    }

    private boolean isShowLink(ConversionContext context, String numberToShow, int totalRow)
    {
        return context.hasProperty("isShowMore") || Integer.parseInt(numberToShow) < totalRow;
    }

    private void setupShowLink(Map<String, Object> contextMap, Map<String, String> parameters, ConversionContext context) throws MacroExecutionException
    {
        contextMap.put("showLink", true);
        String isShowMore = context.getPropertyAsString("isShowMore");
        contextMap.put("isShowMore", isShowMore == null || !Boolean.valueOf(isShowMore));

        contextMap.put("chartId", getNextRefreshId());
        MacroDefinition macroDefinition = new MacroDefinition("jirachart", new RichTextMacroBody(""), null, parameters);
        try
        {
            Streamable out = macroMarshallingFactory.getStorageMarshaller().marshal(macroDefinition, context);
            StringWriter writer = new StringWriter();
            out.writeTo(writer);
            contextMap.put("wikiMarkup", writer.toString());
        }
        catch (XhtmlException e)
        {
            throw new MacroExecutionException("Unable to constract macro definition.", e);
        }
        catch (IOException e)
        {
            throw new MacroExecutionException("Unable to constract macro definition.", e);
        }
        String contentId = context.getEntity() != null ? context.getEntity().getIdAsString() : "-1";
        contextMap.put("contentId", contentId);
    }
}
