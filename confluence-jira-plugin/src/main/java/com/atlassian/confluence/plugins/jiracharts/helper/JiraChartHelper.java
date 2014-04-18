package com.atlassian.confluence.plugins.jiracharts.helper;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.web.UrlBuilder;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JiraChartHelper
{
    public static final String PARAM_JQL = "jql";
    public static final String PARAM_CHART_TYPE = "chartType";
    public static final String PARAM_SERVER_ID = "serverId";
    public static final String PARAM_WIDTH = "width";
    public static final String PARAM_HEIGHT = "height";
    public static final String PARAM_AUTHENTICATED = "authenticated";

    private static final String SERVLET_JIRA_CHART_URI = "/plugins/servlet/jira-chart-proxy";
    private static final List<String> supportedCharts = Arrays.asList("pie", "createdvsresolved");

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
     * @return
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
            if(map.get(parameter) != null)
            {
                urlBuilders.add(parameter, map.get(parameter));
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
     * @param chartType
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
