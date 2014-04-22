package com.atlassian.confluence.plugins.jiracharts.helper;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.renderer.RenderContextOutputType;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JiraChartHelper
{

    public static final String PDF_EXPORT = "pdfExport";
    public static final String PARAM_JQL = "jql";
    public static final String PARAM_CHART_TYPE = "chartType";
    public static final String PARAM_SERVER_ID = "serverId";
    public static final String PARAM_WIDTH = "width";
    public static final String PARAM_HEIGHT = "height";
    public static final String PARAM_AUTHENTICATED = "authenticated";

    private static final String SERVLET_JIRA_CHART_URI = "/plugins/servlet/jira-chart-proxy";
    private static final List<String> supportedCharts = Collections.unmodifiableList(Arrays.asList("pie", "createdvsresolved"));


    /**
     * init a common context map for all chart
     * @param parameters parameters of chart
     * @param result JQLValidationResult
     * @param context ConversionContext
     * @return contextMap
     */
    public static Map<String, Object> getCommonChartContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context)
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

        Boolean isShowBorder = Boolean.parseBoolean(parameters.get("border"));
        Boolean isShowInfor = Boolean.parseBoolean(parameters.get("showinfor"));
        boolean isPreviewMode = ConversionContextOutputType.PREVIEW.name().equalsIgnoreCase(context.getOutputType());
        contextMap.put("jqlValidationResult", result);
        contextMap.put("showBorder", isShowBorder);
        contextMap.put("showInfor", isShowInfor);
        contextMap.put("isPreviewMode", isPreviewMode);

        if (RenderContextOutputType.PDF.equals(context.getOutputType()))
        {
            contextMap.put(PDF_EXPORT, Boolean.TRUE);
        }

        return contextMap;
    }

    /**
     * get the common jira gadget url for all chart
     * @param jql jql query
     * @param width width of chart
     * @param gadgetUrl rest gadget url
     * @return url builder
     */
    public static UrlBuilder getCommonJiraGadgetUrl(String jql, String width, String gadgetUrl)
    {
        String jqlDecodeValue = GeneralUtil.urlDecode(jql);
        UrlBuilder urlBuilder = new UrlBuilder(gadgetUrl + GeneralUtil.urlEncode(jqlDecodeValue, "UTF-8"));
        addSizeParam(urlBuilder, width);
        return urlBuilder;
    }

    /**
     * get the common servlet jira chart url
     * @param params params of chart
     * @param contextPath context path of web
     * @param isAuthenticated authentication user
     * @return servlet url
     */
    public static UrlBuilder getCommonServletJiraChartUrl(Map<String, String> params, String contextPath, boolean isAuthenticated)
    {
        UrlBuilder urlBuilder = new UrlBuilder(contextPath + SERVLET_JIRA_CHART_URI);
        urlBuilder.add(PARAM_JQL, GeneralUtil.urlDecode(params.get(PARAM_JQL)))
                .add(PARAM_SERVER_ID, params.get(PARAM_SERVER_ID))
                .add(PARAM_CHART_TYPE, params.get(PARAM_CHART_TYPE))
                .add(PARAM_AUTHENTICATED, isAuthenticated);

        addSizeParam(urlBuilder, params.get(PARAM_WIDTH));

        return urlBuilder;
    }

    /**
     * add parameter to url
     * @param urlBuilders url builder
     * @param map map parameters
     * @param parameters parameter of chart
     */
    public static void addJiraChartParameter(UrlBuilder urlBuilders, Map<String, String> map, String[] parameters)
    {
        for (String parameter : parameters)
        {
            if (map.get(parameter) != null)
            {
                urlBuilders.add(parameter, map.get(parameter));
            }
        }
    }

    /**
     * add parameter to url
     * @param urlBuilders url builder
     * @param request request
     * @param parameters parameter of chart
     */
    public static void addJiraChartParameter(UrlBuilder urlBuilders, HttpServletRequest request, String[] parameters)
    {
        for (String parameter : parameters)
        {
            if(request.getParameter(parameter) != null)
            {
                urlBuilders.add(parameter, request.getParameterValues(parameter));
            }
        }
    }

    /**
     * check required parameter for chart
     * @param request
     * @return true/false
     */
    public static boolean isRequiredParamValid(HttpServletRequest request)
    {

        return StringUtils.isNotBlank(request.getParameter(PARAM_SERVER_ID))
                && StringUtils.isNotBlank(request.getParameter(PARAM_JQL))
                && StringUtils.isNotBlank(request.getParameter(PARAM_CHART_TYPE));
    }

    /**
     * Check chart type is supported or not
     * @param chartType type of chart
     * @return true/false
     */
    public static boolean isSupportedChart(String chartType)
    {
        return StringUtils.isNotBlank(chartType) && supportedCharts.contains(chartType);
    }

    private static UrlBuilder addSizeParam(UrlBuilder urlBuilder, String width)
    {
        if(StringUtils.isNotBlank(width))
        {
            String height = String.valueOf(Integer.parseInt(width) * 2 / 3);
            urlBuilder.add(PARAM_WIDTH, width)
                    .add(PARAM_HEIGHT, height);
        }
        return urlBuilder;
    }
}
