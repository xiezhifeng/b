package com.atlassian.confluence.plugins.jiracharts;

import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.UrlBuilder;

/**
 * The macro to display Jira chart
 * 
 */
public class JiraChartMacro implements StreamableMacro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String SERVLET_PIE_CHART = "/plugins/servlet/jira-chart-proxy";
    private static final String JIRA_SEARCH_URL = "/rest/api/2/search";
    private static final String TEMPLATE_PATH = "templates/jirachart";

    private ApplicationLinkService applicationLinkService;

    private final MacroExecutorService executorService;
    private I18NBeanFactory i18NBeanFactory;
    private JQLValidator jqlValidator;
    private Settings settings;

    /**
     * JiraChartMacro constructor
     * 
     * @param executorService
     * @param applicationLinkService
     * @param i18NBeanFactory
     */
    public JiraChartMacro(SettingsManager settingManager, MacroExecutorService executorService,
            ApplicationLinkService applicationLinkService,
            I18NBeanFactory i18NBeanFactory)
    {
        this.settings = settingManager.getGlobalSettings();
        this.executorService = executorService;
        this.i18NBeanFactory = i18NBeanFactory;
        this.applicationLinkService = applicationLinkService;
    }

    @Override
    public String execute(Map<String, String> parameters, String body,
            ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = executeInternal(parameters, body,
                context);
        return VelocityUtils.getRenderedTemplate(
                TEMPLATE_PATH + "/piechart.vm", contextMap);
    }

    @Override
    public BodyType getBodyType()
    {
        return BodyType.NONE;
    }

    @Override
    public OutputType getOutputType()
    {
        return OutputType.BLOCK;
    }

    @Override
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters,
            ConversionContext context)
    {
        try
        {
            String jql = GeneralUtil.urlDecode(parameters.get("jql"));
            String statType = parameters.get("statType");
            String serverId = parameters.get("serverId");
            String authenticated = parameters.get("isAuthenticated");
            if (jql != null && statType != null && serverId != null)
            {
                ApplicationLink appLink = applicationLinkService
                        .getApplicationLink(new ApplicationId(serverId));
                if (appLink != null)
                {
                    // Using anonymous user to display chart place holder
                    UrlBuilder urlBuilder = new UrlBuilder(SERVLET_PIE_CHART);
                    urlBuilder.add("jql", jql).add("statType", statType)
                            .add("appId", serverId).add("chartType", "pie")
                            .add("authenticated", authenticated);

                    String url = urlBuilder.toUrl();
                    return new DefaultImagePlaceholder(url, null, false);
                }
            }
        } catch (TypeNotInstalledException e)
        {
            log.error("error don't exist applink", e);
        }
        return null;
    }

    @Override
    public Streamable executeToStream(Map<String, String> parameters,
            Streamable body, ConversionContext context)
            throws MacroExecutionException
    {
        Future<String> futureResult = executorService
                .submit(new StreamableMacroFutureTask(parameters, context,
                        this, AuthenticatedUserThreadLocal.get()));

        return new FutureStreamableConverter.Builder(futureResult, context,
                i18NBeanFactory.getI18NBean())
                .executionErrorMsg("jirachart.error.execution")
                .timeoutErrorMsg("jirachart.error.timeout")
                .interruptedErrorMsg("jirachart.error.interrupted").build();
    }

    public JQLValidator getJqlValidator()
    {
        if (jqlValidator == null)
        {
            this.setJqlValidator(new DefaultJQLValidator(applicationLinkService));
        }
        return jqlValidator;
    }

    public void setJqlValidator(JQLValidator jqlValidator)
    {
        this.jqlValidator = jqlValidator;
    }

    /**
     * Purpose of this method is make JiraChartMarco testable
     * 
     * @param parameters
     * @param body
     * @param context
     * @return The Velocity Context
     * @throws MacroExecutionException
     */
    protected Map<String, Object> executeInternal(
            Map<String, String> parameters, String body,
            ConversionContext context) throws MacroExecutionException
    {
        JQLValidationResult result = getJqlValidator().doValidate(parameters);
        
        String jql = GeneralUtil.urlDecode(parameters.get("jql"));
        String serverId = parameters.get("serverId");
        Boolean isShowBorder = Boolean.parseBoolean(parameters.get("border"));
        Boolean isShowInfor = Boolean.parseBoolean(parameters.get("showinfor"));
        boolean isPreviewMode = ConversionContextOutputType.PREVIEW.name()
                .equalsIgnoreCase(context.getOutputType());
        UrlBuilder urlBuilder = new UrlBuilder(settings.getBaseUrl()
                + SERVLET_PIE_CHART);
        urlBuilder.add("jql", jql).add("statType", parameters.get("statType"))
                .add("appId", serverId).add("chartType", "pie")
                .add("authenticated", !result.isOAuthNeeded());

        String width = parameters.get("width");
        if (!StringUtils.isBlank(width) && Integer.parseInt(width) > 0)
        {
            urlBuilder.add("width", width).add("height",
                    (Integer.parseInt(width) * 2 / 3));
        }
        String url = urlBuilder.toUrl();

        Map<String, Object> contextMap = createVelocityContext();
        contextMap.put("statType", parameters.get("statType"));
        contextMap.put("jqlValidationResult", result);
        contextMap.put("srcImg", url);
        contextMap.put("srcImg", url);
        contextMap.put("showBorder", isShowBorder);
        contextMap.put("showInfor", isShowInfor);
        contextMap.put("isPreviewMode", isPreviewMode);
        return contextMap;
    }

    protected Map<String, Object> createVelocityContext()
    {
        return MacroUtils.defaultVelocityContext();
    }
    
}
