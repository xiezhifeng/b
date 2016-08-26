package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
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
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_JQL;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.getCommonChartContext;

public class TwoDimensionalChart extends JiraHtmlChart
{
    private static final String[] chartParameters = new String[]{"xstattype", "ystattype"};
    private static final String MAX_NUMBER_TO_SHOW_VALUE = "9999";
    private static final String IS_SHOW_MORE_PARAM = "isShowMore";
    private static final String STATUSES_PARAM_VALUE = "statuses";
    private static final String DEFAULT_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jirachart_images/twodimensional-chart-placeholder.png";
    private static final Pattern STATUS_IMG_SRC = Pattern.compile("<img src=\"(.*?)\"");
    private static final Random RANDOM = new Random();

    private MacroMarshallingFactory macroMarshallingFactory;

    public TwoDimensionalChart(ReadOnlyApplicationLinkService applicationLinkService, MacroMarshallingFactory macroMarshallingFactory, I18NBeanFactory i18NBeanFactory)
    {
        this.applicationLinkService = applicationLinkService;
        this.macroMarshallingFactory = macroMarshallingFactory;
        this.i18NBeanFactory = i18NBeanFactory;
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context)
    {
        String numberToShow = getNumberToShow(context, parameters.get("numberToShow"));
        String jql = GeneralUtil.urlDecode(parameters.get(JiraChartHelper.PARAM_JQL));

        Map<String, Object> contextMap = getCommonChartContext(parameters, result, context);
        try
        {
            TwoDimensionalChartModel chart = (TwoDimensionalChartModel) getChartModel(parameters.get(JiraChartHelper.PARAM_SERVER_ID),
                    buildTwoDimensionalRestURL(parameters, numberToShow, jql));
            updateStatusIconLink(parameters, chart, result.getDisplayUrl());
            contextMap.put("chartModel", chart);
            contextMap.put("numberRowShow", getNumberRowShow(numberToShow, chart.getTotalRows()));
            contextMap.put(PARAM_JQL, jql);

            if(isShowLink(context, numberToShow, chart.getTotalRows()))
            {
                setupShowLink(contextMap, parameters, context);
            }

        }
        catch (Exception e)
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
        return DEFAULT_PLACEHOLDER_IMG_PATH;
    }

    @Override
    public String getDefaultImagePlaceholderUrl()
    {
        return DEFAULT_PLACEHOLDER_IMG_PATH;
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
        return RANDOM.nextInt();
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
        catch (XhtmlException | IOException e)
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

    private void updateStatusIconLink(Map<String, String> parameters, TwoDimensionalChartModel chart, String displayURL) throws URISyntaxException
    {
        if(STATUSES_PARAM_VALUE.equals(parameters.get("xstattype")) || STATUSES_PARAM_VALUE.equals(parameters.get("ystattype")))
        {
            String uri = getDisplayURI(displayURL);
            if(STATUSES_PARAM_VALUE.equals(parameters.get("xstattype")))
            {
                List<TwoDimensionalChartModel.Cell> cells = chart.getFirstRow().getCells();
                for (TwoDimensionalChartModel.Cell cell : cells)
                {
                    cell.setMarkup(getStatusMarkup(cell.getMarkup(), uri));
                }
            }

            if(STATUSES_PARAM_VALUE.equals(parameters.get("ystattype")))
            {
                List<TwoDimensionalChartModel.Row> rows = chart.getRows();
                for (TwoDimensionalChartModel.Row row : rows)
                {
                    if(row.getCells().isEmpty()) break;
                    TwoDimensionalChartModel.Cell firstCell = row.getCells().get(0);
                    firstCell.setMarkup(getStatusMarkup(firstCell.getMarkup(), uri));
                }
            }
        }
    }

    private static String getStatusMarkup(String markup, String displayUrl)
    {
        Matcher matcher = STATUS_IMG_SRC.matcher(markup);
        if(matcher.find() && matcher.groupCount() > 0)
        {
            String imgUrl = matcher.group(1);
            if(!imgUrl.matches("^(http://|https://).*"))
            {
                return markup.replace(imgUrl, displayUrl + imgUrl);
            }
        }

        return markup;
    }

    private static String getDisplayURI(String displayURL) throws URISyntaxException
    {
        URI uri = new URI(displayURL);
        return uri.getScheme() + "://" + uri.getAuthority();
    }


}
