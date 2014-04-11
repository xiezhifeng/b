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

    public static UrlBuilder getCommonJiraGadgetUrl(String jql, String width, String gagetUrl)
    {
        String jqlDecodeValue = GeneralUtil.urlDecode(jql);
        UrlBuilder urlBuilder = new UrlBuilder(gagetUrl + GeneralUtil.urlEncode(jqlDecodeValue, "UTF-8"));
        addSizeParam(urlBuilder, width);
        return urlBuilder;
    }

    public static UrlBuilder getCommonServletJiraChartUrl(Map<String, String> params, String baseUrl, boolean isAuthenticated)
    {
        UrlBuilder urlBuilder = new UrlBuilder(baseUrl + SERVLET_JIRA_CHART_URI);
        urlBuilder.add(PARAM_JQL, GeneralUtil.urlDecode(params.get(PARAM_JQL)))
                .add(PARAM_SERVER_ID, params.get(PARAM_SERVER_ID))
                .add(PARAM_AUTHENTICATED, isAuthenticated);
        addSizeParam(urlBuilder, params.get(PARAM_WIDTH));

        return urlBuilder;
    }

    public static boolean isRequiredParamValid(HttpServletRequest request)
    {

        return StringUtils.isNotBlank(request.getParameter(PARAM_SERVER_ID))
                && StringUtils.isNotBlank(request.getParameter(PARAM_JQL))
                && StringUtils.isNotBlank(request.getParameter(PARAM_CHART_TYPE));
    }

    private static UrlBuilder addSizeParam(UrlBuilder urlBuilder, String width)
    {
        width = StringUtils.isBlank(width) ? CHART_PDF_EXPORT_WIDTH_DEFAULT : width;
        String height = String.valueOf(Integer.parseInt(width) * 2 / 3);
        urlBuilder.add(PARAM_WIDTH, width)
                  .add(PARAM_HEIGHT, height);

        return urlBuilder;
    }
}
