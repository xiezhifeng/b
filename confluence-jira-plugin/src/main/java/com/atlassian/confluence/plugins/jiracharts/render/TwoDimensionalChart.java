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
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_JQL;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.getCommonChartContext;

public class TwoDimensionalChart extends JiraHtmlChart
{

    private static final String[] chartParameters = new String[]{"xstattype", "ystattype"};
    private static final String MAX_NUMBER_TO_SHOW_VALUE = "9999";
    private static final String IS_SHOW_MORE_PARAM = "isShowMore";

    private MacroMarshallingFactory macroMarshallingFactory;

    public TwoDimensionalChart(ApplicationLinkService applicationLinkService, MacroMarshallingFactory macroMarshallingFactory, I18NBeanFactory i18NBeanFactory)
    {
        this.applicationLinkService = applicationLinkService;
        this.macroMarshallingFactory = macroMarshallingFactory;
        this.i18NBeanFactory = i18NBeanFactory;
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        String numberToShow = getNumberToShow(context, parameters.get("numberToShow"));
        String jql = GeneralUtil.urlDecode(parameters.get(JiraChartHelper.PARAM_JQL));

        Map<String, Object> contextMap = getCommonChartContext(parameters, result, context);
        try
        {
            TwoDimensionalChartModel chart = (TwoDimensionalChartModel) getChartModel(parameters.get(JiraChartHelper.PARAM_SERVER_ID),
                    buildTwoDimensionalRestURL(parameters, numberToShow, jql));
            contextMap.put("chartModel", chart);
            contextMap.put("numberRowShow", getNumberRowShow(numberToShow, chart.getTotalRows()));
            contextMap.put(PARAM_JQL, jql);

            if(isShowLink(context, numberToShow, chart.getTotalRows()))
            {
                setupShowLink(contextMap, parameters, context);
            }

        }
        catch (MacroExecutionException e)
        {
            contextMap.put("error", e.getMessage());
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
        if(context.hasProperty(IS_SHOW_MORE_PARAM) && Boolean.valueOf(context.getPropertyAsString(IS_SHOW_MORE_PARAM)))
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
        return context.hasProperty(IS_SHOW_MORE_PARAM) || Integer.parseInt(numberToShow) < totalRow;
    }

    private void setupShowLink(Map<String, Object> contextMap, Map<String, String> parameters, ConversionContext context) throws MacroExecutionException
    {
        contextMap.put("showLink", true);
        String isShowMore = context.getPropertyAsString(IS_SHOW_MORE_PARAM);
        contextMap.put(IS_SHOW_MORE_PARAM, isShowMore == null || !Boolean.valueOf(isShowMore));

        contextMap.put("chartId", getNextRefreshId());

        //TODO: Will extract to common function for jira chart and jira issue macro
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
            throw new MacroExecutionException("Unable to construct macro definition.", e);
        }
        catch (IOException e)
        {
            throw new MacroExecutionException("Unable to construct macro definition.", e);
        }
        String contentId = context.getEntity() != null ? context.getEntity().getIdAsString() : "-1";
        contextMap.put("contentId", contentId);
    }

    private String buildTwoDimensionalRestURL(Map<String, String> parameters, String numberToShow, String jql)
    {
        UrlBuilder urlBuilder = new UrlBuilder(getJiraGadgetRestUrl() + GeneralUtil.urlEncode(jql, "UTF-8"));
        JiraChartHelper.addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        urlBuilder.add("sortBy", "natural");
        urlBuilder.add("showTotals", true);
        urlBuilder.add("numberToShow", numberToShow);
        urlBuilder.add("sortDirection", "asc");
        return urlBuilder.toString();
    }

    private String getNumberRowShow(String numberToShow, int totalRows)
    {
        if (StringUtils.isNumeric(numberToShow) && Integer.parseInt(numberToShow) > totalRows)
        {
            return String.valueOf(totalRows);
        }

        return numberToShow;
    }
}
