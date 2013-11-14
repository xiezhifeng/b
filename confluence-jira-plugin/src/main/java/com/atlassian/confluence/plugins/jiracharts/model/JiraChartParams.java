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

    private String jql;
    private String statType;
    private String chartType;
    private String appId;
    private String width;
    private String height;

    public JiraChartParams()
    {

    }

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
        this.chartType = chartType.getName();
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
        UrlBuilder urlBuilder = new UrlBuilder(chartType.getJiraChartUrl() + jql);
        urlBuilder.add(PARAM_STAT_TYPE, statType);
        if(width != null)
        {
            urlBuilder.add(PARAM_WIDTH, width);
        }
        if(height != null)
        {
            urlBuilder.add(PARAM_HEIGHT, height);
        }
        return urlBuilder.toString();
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

    public String getJql()
    {
        return jql;
    }

    public void setJql(String jql)
    {
        this.jql = jql;
    }

    public String getStatType()
    {
        return statType;
    }

    public void setStatType(String statType)
    {
        this.statType = statType;
    }

    public String getChartType()
    {
        return chartType;
    }

    public void setChartType(String chartType)
    {
        this.chartType = chartType;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public String getWidth()
    {
        return width;
    }

    public void setWidth(String width)
    {
        this.width = width;
    }

    public String getHeight()
    {
        return height;
    }

    public void setHeight(String height)
    {
        this.height = height;
    }
}
