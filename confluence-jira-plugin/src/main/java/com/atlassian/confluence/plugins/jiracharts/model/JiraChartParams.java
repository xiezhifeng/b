package com.atlassian.confluence.plugins.jiracharts.model;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.web.UrlBuilder;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class JiraChartParams
{
    private static final String PARAM_JQL = "jql";
    public static final String PARAM_CHART_TYPE = "chartType";
    private static final String PARAM_SERVER_ID = "serverId";
    private static final String PARAM_WIDTH = "width";
    private static final String PARAM_HEIGHT = "height";
    private static final String PARAM_AUTHENTICATED = "authenticated";

    private static final String CHART_PDF_EXPORT_WIDTH_DEFAULT = "320";

    private static final String SERVLET_JIRA_CHART_URI = "/plugins/servlet/jira-chart-proxy";

    public static String buildJiraGadgetUrl(HttpServletRequest request)
    {
        String jqlDecodeValue = GeneralUtil.urlDecode(request.getParameter(PARAM_JQL));
        ChartType chartType = ChartType.getChartType(request.getParameter(PARAM_CHART_TYPE));

        UrlBuilder urlBuilder = new UrlBuilder(chartType.getJiraChartUrl() + GeneralUtil.urlEncode(jqlDecodeValue, "UTF-8"));
        addExtendedParams(urlBuilder, chartType, request);
        addSizeParam(urlBuilder, request.getParameter(PARAM_WIDTH));
        return urlBuilder.toString();
    }

    public static String buildJiraGadgetUrl(Map<String, String> params)
    {
        String jqlDecodeValue = GeneralUtil.urlDecode(params.get(PARAM_JQL));
        ChartType chartType = ChartType.getChartType(params.get(PARAM_CHART_TYPE));

        UrlBuilder urlBuilder = new UrlBuilder(chartType.getJiraChartUrl() + GeneralUtil.urlEncode(jqlDecodeValue, "UTF-8"));
        addExtendedParams(urlBuilder, chartType, params);
        addSizeParam(urlBuilder, params.get(PARAM_WIDTH));
        return urlBuilder.toString();
    }

    public static String buildServletJiraChartUrl(Map<String, String> params, String baseUrl, boolean isAuthenticated)
    {
        UrlBuilder urlBuilder = new UrlBuilder(baseUrl + SERVLET_JIRA_CHART_URI);
        urlBuilder.add(PARAM_JQL, GeneralUtil.urlDecode(params.get(PARAM_JQL)))
                  .add(PARAM_SERVER_ID, params.get(PARAM_SERVER_ID))
                  .add(PARAM_AUTHENTICATED, isAuthenticated);

        ChartType chartType = ChartType.getChartType(params.get(PARAM_CHART_TYPE));
        addExtendedParams(urlBuilder, chartType, params);
        addSizeParam(urlBuilder, params.get(PARAM_WIDTH));

        return urlBuilder.toString();
    }

    public static boolean isRequiredParamValid(HttpServletRequest request)
    {
        ChartType chartType = ChartType.getChartType(request.getParameter(PARAM_CHART_TYPE));
        return StringUtils.isNotBlank(request.getParameter(PARAM_SERVER_ID))
                && StringUtils.isNotBlank(request.getParameter(PARAM_JQL))
                && chartType != null;
    }

    private static UrlBuilder addSizeParam(UrlBuilder urlBuilder, String width)
    {
        width = StringUtils.isBlank(width) ? CHART_PDF_EXPORT_WIDTH_DEFAULT : width;
        String height = String.valueOf(Integer.parseInt(width) * 2 / 3);
        urlBuilder.add(PARAM_WIDTH, width)
                  .add(PARAM_HEIGHT, height);

        return urlBuilder;
    }

    private static UrlBuilder addExtendedParams(UrlBuilder urlBuilder, ChartType chartType, Map<String, String> params)
    {
        for(String param : chartType.getExtendedParams())
        {
            if(params.get(param) != null)
            {
                urlBuilder.add(param, params.get(param));
            }
        }

        return urlBuilder;
    }

    private static UrlBuilder addExtendedParams(UrlBuilder urlBuilder, ChartType chartType, HttpServletRequest request)
    {
        for(String param : chartType.getExtendedParams())
        {
            if(request.getParameter(param) != null)
            {
                urlBuilder.add(param, request.getParameter(param));
            }
        }

        return urlBuilder;
    }
}
