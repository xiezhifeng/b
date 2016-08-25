package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.model.JiraImageChartModel;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.*;

public abstract class JiraImageChart implements JiraChart
{
    private static Logger log = LoggerFactory.getLogger(JiraImageChart.class);

    protected ContextPathHolder pathHolder;

    protected Base64JiraChartImageService base64JiraChartImageService;

    protected final String SOURCE_IMAGE_PARAM = "srcImg";

    /**
     * get gadget url base on request params
     * @param request http request
     * @return gadget url
     */
    public abstract String getJiraGadgetUrl(HttpServletRequest request);

    /**
     *
     * @return default value when pdf render
     */
    public abstract String getDefaultPDFChartWidth();


    /**
     *
     * @param parameters parameters
     * @param outputType type of view render PDF/PAGE/...
     * @return JiraImageChartModel
     * @throws MacroExecutionException
     */
    protected JiraImageChartModel getImageSourceModel(Map<String, String> parameters, String outputType) throws MacroExecutionException
    {
        try
        {
            String width = parameters.get(PARAM_WIDTH);
            if (RenderContextOutputType.PDF.equals(outputType) && StringUtils.isBlank(width))
            {
                width = getDefaultPDFChartWidth();
            }

            UrlBuilder urlBuilder = getCommonJiraGadgetUrl(parameters.get(PARAM_JQL), width, getJiraGadgetRestUrl());
            addJiraChartParameter(urlBuilder, parameters, getChartParameters());
            return base64JiraChartImageService.getBase64JiraChartImageModel(parameters.get(PARAM_SERVER_ID), urlBuilder.toString());
        }
        catch(ResponseException e)
        {
            log.debug("Can not retrieve jira chart image for export pdf");
            throw new MacroExecutionException(e);
        }
    }

    @Override
    public boolean isVerifyChartSupported()
    {
        return true;
    }
}
