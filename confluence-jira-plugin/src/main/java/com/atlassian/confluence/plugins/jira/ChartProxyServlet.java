package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.extra.jira.model.JiraChartModel;
import com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChartFactory;
import com.atlassian.confluence.plugins.jiracharts.render.JiraImageChart;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class ChartProxyServlet extends AbstractProxyServlet
{
    
    private static final Logger log = Logger.getLogger(ChartProxyServlet.class);

    private final JiraChartFactory jiraChartFactory;
    
    public ChartProxyServlet(ApplicationLinkService appLinkService, JiraChartFactory jiraChartFactory)
    {
        super(appLinkService);
        this.jiraChartFactory = jiraChartFactory;
    }
    
    @Override
    void doProxy(HttpServletRequest req, HttpServletResponse resp, MethodType methodType) throws IOException, ServletException
    {
        if(JiraChartHelper.isRequiredParamValid(req))
        {
            String chartType = req.getParameter("chartType");
            JiraImageChart jiraChart = (JiraImageChart)jiraChartFactory.getJiraChartRenderer(chartType);
            super.doProxy(resp, req, methodType, jiraChart.getJiraGadgetUrl(req));
        }
        else
        {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Either jql, chartType or appId parameters is empty");
        }

    }
    
    @Override
    protected void handleResponse(ApplicationLinkRequestFactory requestFactory, HttpServletRequest req, HttpServletResponse resp, ApplicationLinkRequest request, ApplicationLink appLink) throws ResponseException
    {
        String redirectLink = getRedirectImgLink(request, req, requestFactory, resp, appLink);
        if(redirectLink != null)
        {
            try
            {
                resp.sendRedirect(redirectLink);
            }
            catch (IOException e)
            {
                log.error("unable to send redirect to " + redirectLink, e);
            }
        }
    }
    
    protected String getRedirectImgLink(ApplicationLinkRequest request, HttpServletRequest req, ApplicationLinkRequestFactory requestFactory, HttpServletResponse resp, ApplicationLink appLink) throws ResponseException
    {
        ChartProxyResponseHandler responseHandler = new ChartProxyResponseHandler(req, requestFactory, resp);
        Object ret = request.execute(responseHandler);
        if (ret != null && ret instanceof ByteArrayOutputStream)
        {
            ByteArrayInputStream in = new ByteArrayInputStream(((ByteArrayOutputStream) ret).toByteArray());
            JiraChartModel chartModel = null;
            try
            {
                chartModel = GsonHolder.gson.fromJson(new InputStreamReader(in), JiraChartModel.class);
            }
            catch (Exception e)
            {
                log.error("Unable to parse jira chart macro json to object", e);
            }

            if (chartModel != null && chartModel.getLocation() != null)
            {
                return appLink.getDisplayUrl() + "/charts?filename=" + chartModel.getLocation();
            }
        }
        return null;
    }
    
    /**
     * Gson is thead-safe, so just use shared instance for all thread
     */
    private static final class GsonHolder
    {
        static final Gson gson = new Gson();
    }
    
    protected static class ChartProxyResponseHandler extends ProxyApplicationLinkResponseHandler
    {

        private ChartProxyResponseHandler(HttpServletRequest req,
                ApplicationLinkRequestFactory requestFactory, HttpServletResponse resp)
        {
            super(req, requestFactory, resp);
        }
        
        @Override
        protected Object processSuccess(Response response) throws ResponseException
        {
            InputStream responseStream = response.getResponseBodyAsStream();
            Map<String, String> headers = response.getHeaders();
            for (String key : headers.keySet())
            {
                if (headerWhitelist.contains(key))
                {
                    resp.setHeader(key, headers.get(key));
                }
            }
            try
            {
                if (responseStream != null)
                {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copy(responseStream, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    return outputStream;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            return null;
        }        
    }
}