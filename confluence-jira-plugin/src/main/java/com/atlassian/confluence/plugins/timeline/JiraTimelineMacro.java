package com.atlassian.confluence.plugins.timeline;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.extra.jira.JiraIssuesColumnManager;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraIssuesXmlTransformer;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.json.json.Json;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.*;
import com.atlassian.confluence.plugins.jiracharts.Base64JiraChartImageService;
import com.atlassian.confluence.plugins.jiracharts.DefaultJQLValidator;
import com.atlassian.confluence.plugins.jiracharts.JQLValidator;
import com.atlassian.confluence.plugins.jiracharts.JiraStatType;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.model.JiraChartParams;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;

/**
 * Created by khoa.pham on 2/26/14.
 */
public class JiraTimelineMacro implements StreamableMacro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraTimelineMacro.class);
    private static final String IMAGE_GENERATOR_SERVLET = "/plugins/servlet/image-generator";
    private static final String TEMPLATE_PATH = "templates/jirachart";
    private static final String JIRA_CHART_DEFAULT_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jirachart_images/jirachart_placeholder.png";
    private ApplicationLinkService applicationLinkService;

    private final MacroExecutorService executorService;
    private I18NBeanFactory i18NBeanFactory;
    private JQLValidator jqlValidator;
    private Settings settings;
    private Base64JiraChartImageService base64JiraChartImageService;
    private JiraConnectorManager jiraConnectorManager;
    private LocaleManager localeManager;
    private FormatSettingsManager formatSettingsManager;

    private static final String PDF_EXPORT = "pdfExport";
    private static final String CHART_PDF_EXPORT_WIDTH_DEFAULT = "320";

    private JiraIssuesManager jiraIssuesManager;


    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();
    /**
     * JiraChartMacro constructor
     *
     * @param executorService
     * @param applicationLinkService
     * @param i18NBeanFactory
     */
    public JiraTimelineMacro(SettingsManager settingManager, MacroExecutorService executorService,
                          ApplicationLinkService applicationLinkService, I18NBeanFactory i18NBeanFactory,
                          Base64JiraChartImageService base64JiraChartImageService, JiraConnectorManager jiraConnectorManager, JiraIssuesManager jiraIssuesManager,
                          LocaleManager localeManager, FormatSettingsManager formatSettingsManager)
    {
        this.settings = settingManager.getGlobalSettings();
        this.executorService = executorService;
        this.i18NBeanFactory = i18NBeanFactory;
        this.applicationLinkService = applicationLinkService;
        this.base64JiraChartImageService = base64JiraChartImageService;
        this.jiraConnectorManager = jiraConnectorManager;
        this.jiraIssuesManager = jiraIssuesManager;
        this.localeManager = localeManager;
        this.formatSettingsManager = formatSettingsManager;
    }

    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context)
            throws MacroExecutionException
    {
        String jql = parameters.get("jql");

        //get primary server
        ApplicationLink applicationLink = applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class);

        StringBuffer sf = new StringBuffer(JiraUtil.normalizeUrl(applicationLink.getRpcUrl()));
        sf.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?jqlQuery=");
        sf.append(JiraUtil.utf8Encode(jql));
        String url = sf.toString();
        List<String> columns = Arrays.asList(
                "description", "environment", "key", "summary", "type", "parent",
                "priority", "status", "version", "resolution", "security", "assignee", "reporter",
                "created", "updated", "due", "component", "components", "votes", "comments", "attachments",
                "subtasks", "fixversion", "timeoriginalestimate", "timeestimate","allcustom"
        );

        Locale locale = localeManager.getLocale(AuthenticatedUserThreadLocal.get());
        Map map = MacroUtils.defaultVelocityContext();
        map.put("dateFormat", new SimpleDateFormat(formatSettingsManager.getDateFormat(), locale));
        map.put("xmlXformer", xmlXformer);
        map.put("group", parameters.get("group"));
        map.put("id", "time" + Calendar.getInstance().getTimeInMillis());
        map.put("appId", applicationLink.getId().toString());
        map.put("width", StringUtils.isBlank(parameters.get("width")) ? "100%" : parameters.get("width"));
        map.put("height", StringUtils.isBlank(parameters.get("height")) ? "400px" : parameters.get("height"));

        try
        {
            String versions = "";
            if(jql.toLowerCase().indexOf("project") > -1) {
                versions =  jiraIssuesManager.retrieveVersions(applicationLink, jql.split("=")[1].toString());
                if(StringUtils.isNotBlank(versions))
                {
                    JsonObject jsonObject = (JsonObject) new JsonParser().parse(versions);
                    versions = jsonObject.get("versions").toString();
                }
            }

            map.put("versions", versions);
        }
        catch (Exception e)
        {
            map.put("versions", "");
        }

        try
        {

            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columns, applicationLink, false, false);
            Element element = channel.getChannelElement();
            List<Element> elements = element.getChildren("item");
            map.put("entries", elements);
        }
        catch (CredentialsRequiredException e)
        {

        }
        catch (ResponseException e)
        {

        }
        catch (Exception e)
        {

        }

        map.put("group", parameters.get("group"));

        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/timeline.vm", map);
    }

    @Override
    public Macro.BodyType getBodyType()
    {
        return Macro.BodyType.NONE;
    }

    @Override
    public Macro.OutputType getOutputType()
    {
        return Macro.OutputType.BLOCK;
    }

    @Override
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext context)
    {
        try
        {
            JQLValidationResult result = getJqlValidator().doValidate(parameters);
            if (result.isOAuthNeeded())
            {
                return null;
            }

            String jql = GeneralUtil.urlDecode(parameters.get("jql"));
            String statType = parameters.get("statType");
            String serverId = parameters.get("serverId");
            String authenticated = parameters.get("isAuthenticated");

            if (authenticated == null)
            {
                authenticated = "false";
            }

            if (jql != null && statType != null && serverId != null)
            {
                ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(serverId));
                if (appLink != null)
                {
                    UrlBuilder urlBuilder = new UrlBuilder(IMAGE_GENERATOR_SERVLET);
                    urlBuilder.add("macro", "jirachart").add("jql", jql).add("statType", statType).add("appId", serverId).add("chartType", "pie")
                            .add("authenticated", authenticated);

                    String url = urlBuilder.toUrl();
                    return new DefaultImagePlaceholder(url, null, false);
                }

            }
        }
        catch (TypeNotInstalledException e)
        {
            log.error("error don't exist applink", e);
        }
        catch (Exception e)
        {
            log.error("error get image place holder", e);
        }

        return new DefaultImagePlaceholder(JIRA_CHART_DEFAULT_PLACEHOLDER_IMG_PATH, null, false);
    }

    @Override
    public Streamable executeToStream(Map<String, String> parameters, Streamable body, ConversionContext context)
            throws MacroExecutionException
    {
        Future<String> futureResult = executorService.submit(new StreamableMacroFutureTask(parameters, context, this,
                AuthenticatedUserThreadLocal.get()));

        return new FutureStreamableConverter.Builder(futureResult, context, i18NBeanFactory.getI18NBean())
                .executionErrorMsg("jirachart.error.execution")
                .executionTimeoutErrorMsg("jirachart.error.timeout.execution")
                .connectionTimeoutErrorMsg("jirachart.error.timeout.connection")
                .interruptedErrorMsg("jirachart.error.interrupted").build();
    }

    public JQLValidator getJqlValidator()
    {
        if (jqlValidator == null)
        {
            this.setJqlValidator(new DefaultJQLValidator(applicationLinkService, i18NBeanFactory, jiraConnectorManager));
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
    protected Map<String, Object> executeInternal(Map<String, String> parameters, String body, ConversionContext context)
            throws MacroExecutionException, TypeNotInstalledException
    {

        JQLValidationResult result = getJqlValidator().doValidate(parameters);
        return setupContext(parameters, result, context);
    }


    /**
     *
     * @param parameters parameters of jira chart macro
     * @param result JQLValidationResult
     * @param context ConversionContext
     * @return context map for view page
     */
    private Map<String, Object> setupContext(Map<String, String> parameters, JQLValidationResult result, ConversionContext context)
            throws MacroExecutionException
    {
        //TODO: will refactor when get more params information to setup for each jira chart
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

    private String getImageSource(String outputType, Map<String, String> parameters, boolean isAuthenticated) throws MacroExecutionException
    {
        JiraChartParams params = new JiraChartParams(parameters);
        return params.buildServletJiraChartUrl(settings.getBaseUrl(), isAuthenticated);
    }
}
