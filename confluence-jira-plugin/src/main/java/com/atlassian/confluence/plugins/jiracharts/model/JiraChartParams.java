package com.atlassian.confluence.plugins.jiracharts.model;

import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.web.UrlBuilder;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class JiraChartParams
{
    private static final String PARAM_JQL = "jql";
    private static final String PARAM_STAT_TYPE = "statType";
    private static final String PARAM_CHART_TYPE = "chartType";
    private static final String PARAM_APP_ID = "appId";
    private static final String PARAM_SERVER_ID = "serverId";
    private static final String PARAM_WIDTH = "width";
    private static final String PARAM_HEIGHT = "height";
    private static final String PARAM_AUTHENTICATED = "authenticated";

    private static final String SERVLET_JIRA_CHART_URI = "/plugins/servlet/jira-chart-proxy";

    public enum ChartType {

        PIE_CHART("pie", "/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-");

        private final String url;
        private final String name;

        ChartType(String name, String url)
        {
            this.url = url;
            this.name = name;
        }
    }

    private String jql;
    private String statType;
    private String chartType;
    private String appId;
    private String width;
    private String height;

    public JiraChartParams(HttpServletRequest req)
    {
        if(req.getParameter(PARAM_JQL) != null)
        {
            this.jql = GeneralUtil.urlEncode(req.getParameter(PARAM_JQL), "UTF-8");
        }
        this.chartType = req.getParameter(PARAM_CHART_TYPE);
        this.appId = req.getParameter(PARAM_APP_ID);
        this.statType = req.getParameter(PARAM_STAT_TYPE);
        this.width = req.getParameter(PARAM_WIDTH);
        this.height = req.getParameter(PARAM_HEIGHT);
    }

    public JiraChartParams(Map<String, String> parameters, ChartType chartType)
    {
        if(parameters.get(PARAM_JQL) != null)
        {
            this.jql = GeneralUtil.urlDecode(parameters.get(PARAM_JQL));
        }
        this.chartType = chartType.name;
        this.appId = parameters.get(PARAM_SERVER_ID);
        this.statType = parameters.get(PARAM_STAT_TYPE);
        String widthParam = parameters.get(PARAM_WIDTH);
        if (StringUtils.isNumeric(widthParam) && Integer.parseInt(widthParam) > 0)
        {
            this.width = widthParam;
            this.height = String.valueOf(Integer.parseInt(width) * 2 / 3);
        }
    }

    public String buildJiraGadgetUrl(ChartType chartType)
    {
        StringBuilder paramsBuilder = new StringBuilder();
        paramsBuilder.append(jql);

        paramsBuilder.append("&statType=").append(statType);
        if(width != null)
        {
            paramsBuilder.append("&width=").append(width);
        }
        if(height != null)
        {
            paramsBuilder.append("&height=").append(height);
        }

        return chartType.url + paramsBuilder.toString();
    }

    public String buildServletJiraChartUrl(String baseUrl, boolean isAuthenticated)
    {
        UrlBuilder urlBuilder = new UrlBuilder(baseUrl + SERVLET_JIRA_CHART_URI);
        urlBuilder.add(PARAM_JQL, GeneralUtil.urlDecode(jql))
                  .add(PARAM_STAT_TYPE, statType)
                  .add(PARAM_APP_ID, appId)
                  .add(PARAM_CHART_TYPE, chartType)
                  .add(PARAM_AUTHENTICATED, isAuthenticated);

        if (StringUtils.isNotBlank(width))
        {
            urlBuilder.add(PARAM_WIDTH, width)
                      .add(PARAM_HEIGHT, height);
        }

        return urlBuilder.toString();
    }

    public boolean isRequiredParamValid()
    {
        return StringUtils.isNotBlank(appId) && StringUtils.isNotBlank(jql) && StringUtils.isNotBlank(chartType);
    }
}
