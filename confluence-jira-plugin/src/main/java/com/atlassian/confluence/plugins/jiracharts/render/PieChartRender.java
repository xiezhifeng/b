package com.atlassian.confluence.plugins.jiracharts.render;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.JiraStatType;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
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

import static com.atlassian.confluence.plugins.jiracharts.model.JiraChartParams.*;

public class PieChartRender implements JiraChartRenderer
{

    private static Logger log = LoggerFactory.getLogger(PieChartRender.class);
    private static final String PDF_EXPORT = "pdfExport";

    private Base64JiraChartImageService base64JiraChartImageService;
    private I18NBeanFactory i18NBeanFactory;
    private Settings settings;

    private static String[] chartParameters = new String[]{"statType"};

    public PieChartRender(SettingsManager settingManager, I18NBeanFactory i18NBeanFactory, Base64JiraChartImageService base64JiraChartImageService)
    {
        this.i18NBeanFactory = i18NBeanFactory;
        this.settings = settingManager.getGlobalSettings();
        this.base64JiraChartImageService = base64JiraChartImageService;
    }

    @Override
    public String[] getChartParameters()
    {
        return chartParameters;
    }

    @Override
    public String getTemplateFileName()
    {
        return "piechart.vm";
    }

    @Override
    public String getJiraGagetUrl()
    {
        return "/rest/gadget/1.0/piechart/generate?projectOrFilterId=jql-";
    }

    @Override
    public String getJiraGagetUrl(HttpServletRequest request)
    {
        UrlBuilder urlBuilder = getCommonJiraGadgetUrl(request.getParameter(PARAM_JQL), request.getParameter(PARAM_WIDTH), getJiraGagetUrl());
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
    public String getImagePlaceholderUrl(Map<String, String> parameters, UrlBuilder urlBuilder)
    {
        addJiraChartParameter(urlBuilder, parameters, getChartParameters());
        return urlBuilder.toString();
    }

    private String getImageSource(String outputType, Map<String, String> parameters, boolean isAuthenticated) throws MacroExecutionException
    {
        if (RenderContextOutputType.PDF.equals(outputType))
        {
            try
            {
                UrlBuilder urlBuilder = getCommonJiraGadgetUrl(parameters.get(PARAM_JQL), parameters.get(PARAM_WIDTH), getJiraGagetUrl());
                addJiraChartParameter(urlBuilder, parameters, getChartParameters());
                return base64JiraChartImageService.getBase64JiraChartImage(parameters.get(PARAM_SERVER_ID), urlBuilder.toString());
            }
            catch (ResponseException e)
            {
                log.debug("Can not retrieve jira chart image for export pdf");
                throw new MacroExecutionException(e);
            }
        }
        else
        {
            UrlBuilder urlBuilder = getCommonServletJiraChartUrl(parameters, settings.getBaseUrl(), isAuthenticated);
            addJiraChartParameter(urlBuilder, parameters, getChartParameters());
            return urlBuilder.toString();
        }
    }
}
