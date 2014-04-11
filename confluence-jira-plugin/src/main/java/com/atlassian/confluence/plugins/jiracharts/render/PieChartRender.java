package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.JiraStatType;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.JiraChartParams;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.sal.api.net.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class PieChartRender implements JiraChartRenderer
{

    private static Logger log = LoggerFactory.getLogger(PieChartRender.class);
    private static final String PDF_EXPORT = "pdfExport";

    private Base64JiraChartImageService base64JiraChartImageService;
    private I18NBeanFactory i18NBeanFactory;
    private Settings settings;

    public PieChartRender(SettingsManager settingManager, I18NBeanFactory i18NBeanFactory, Base64JiraChartImageService base64JiraChartImageService)
    {
        this.i18NBeanFactory = i18NBeanFactory;
        this.settings = settingManager.getGlobalSettings();
        this.base64JiraChartImageService = base64JiraChartImageService;
    }

    @Override
    public String getJiraGagetUrl()
    {
        return "/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-";
    }

    @Override
    public String getJiraGagetUrl(HttpServletRequest request)
    {
        UrlBuilder urlBuilder = JiraChartParams.getCommonJiraGadgetUrl(request.getParameter("jql"), request.getParameter("width"), getJiraGagetUrl());
        urlBuilder.add("statType", request.getParameter("statType"));
        return urlBuilder.toString();
    }

    @Override
    public Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

        Boolean isShowBorder = Boolean.parseBoolean(parameters.get("border"));
        Boolean isShowInfor = Boolean.parseBoolean(parameters.get("showinfor"));
        boolean isPreviewMode = ConversionContextOutputType.PREVIEW.name().equalsIgnoreCase(context.getOutputType());
        String statType = parameters.get("statType");
        String statTypeI18N = i18NBeanFactory.getI18NBean().getText(JiraStatType.getByJiraKey(statType).getResourceKey());
        contextMap.put("statType", statTypeI18N);
        contextMap.put("jqlValidationResult", result);
        contextMap.put("showBorder", isShowBorder);
        contextMap.put("showInfor", isShowInfor);
        contextMap.put("isPreviewMode", isPreviewMode);
        contextMap.put("srcImg", getImageSource(context.getOutputType(), parameters, !result.isOAuthNeeded()));

        if (RenderContextOutputType.PDF.equals(context.getOutputType()))
        {
            contextMap.put(PDF_EXPORT, Boolean.TRUE);
        }

        return contextMap;
    }

    @Override
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext context)
    {
        return null;
    }

    private String getImageSource(String outputType, Map<String, String> parameters, boolean isAuthenticated) throws MacroExecutionException
    {
        if (RenderContextOutputType.PDF.equals(outputType))
        {
            try
            {
                UrlBuilder urlBuilder = JiraChartParams.getCommonJiraGadgetUrl(parameters.get("jql"), parameters.get("width"), getJiraGagetUrl());
                urlBuilder.add("statType", parameters.get("statType"));
                return base64JiraChartImageService.getBase64JiraChartImage(parameters.get("serverId"), urlBuilder.toString());
            }
            catch (ResponseException e)
            {
                log.debug("Can not retrieve jira chart image for export pdf");
                throw new MacroExecutionException(e);
            }
        }
        else
        {
            UrlBuilder urlBuilder = JiraChartParams.getCommonServletJiraChartUrl(parameters, settings.getBaseUrl(), isAuthenticated);
            urlBuilder.add("statType", parameters.get("statType"));

            return urlBuilder.toString();
        }
    }
}
