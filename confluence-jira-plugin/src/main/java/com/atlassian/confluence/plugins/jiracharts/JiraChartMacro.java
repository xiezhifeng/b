package com.atlassian.confluence.plugins.jiracharts;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.macro.*;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChartFactory;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChart;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.UrlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Future;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.*;

/**
 * The macro to display Jira chart
 *
 */
public class JiraChartMacro implements StreamableMacro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String IMAGE_GENERATOR_SERVLET = "/plugins/servlet/image-generator";
    private static final String TEMPLATE_PATH = "templates/jirachart/";
    private static final String JIRA_CHART_DEFAULT_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jirachart_images/jirachart_placeholder.png";
    private ApplicationLinkService applicationLinkService;

    private final MacroExecutorService executorService;
    private I18NBeanFactory i18NBeanFactory;
    private JQLValidator jqlValidator;


    private JiraConnectorManager jiraConnectorManager;

    private JiraChartFactory jiraChartFactory;



    /**
     * JiraChartMacro constructor
     *
     * @param executorService
     * @param applicationLinkService
     * @param i18NBeanFactory
     */
    public JiraChartMacro(MacroExecutorService executorService, ApplicationLinkService applicationLinkService, I18NBeanFactory i18NBeanFactory,
            JiraConnectorManager jiraConnectorManager, JiraChartFactory jiraChartFactory)
    {
        this.executorService = executorService;
        this.i18NBeanFactory = i18NBeanFactory;
        this.applicationLinkService = applicationLinkService;
        this.jiraConnectorManager = jiraConnectorManager;
        this.jiraChartFactory = jiraChartFactory;
    }

    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context)
            throws MacroExecutionException
    {
        JQLValidationResult result = getJqlValidator().doValidate(parameters);
        String chartType = parameters.get(PARAM_CHART_TYPE);
        JiraChart jiraChart = jiraChartFactory.getJiraChartRenderer(chartType);
        Map<String, Object> contextMap = jiraChart.setupContext(parameters, result, context);

        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + jiraChart.getTemplateFileName(), contextMap);
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
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext context)
    {
        try
        {
            JQLValidationResult result = getJqlValidator().doValidate(parameters);
            if (result.isOAuthNeeded())
            {
                return null;
            }

            String jql = GeneralUtil.urlDecode(parameters.get(PARAM_JQL));
            String serverId = parameters.get(PARAM_SERVER_ID);
            String chartType = parameters.get(PARAM_CHART_TYPE);
            String authenticated = parameters.get(PARAM_AUTHENTICATED);
            if (authenticated == null)
            {
                authenticated = "false";
            }

            if (jql != null && serverId != null)
            {
                ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(serverId));
                JiraChart jiraChart = jiraChartFactory.getJiraChartRenderer(chartType);
                if (appLink != null && jiraChart != null)
                {
                    UrlBuilder urlBuilder = new UrlBuilder(IMAGE_GENERATOR_SERVLET);
                    urlBuilder.add("macro", "jirachart")
                              .add(PARAM_JQL, jql)
                              .add(PARAM_SERVER_ID, serverId)
                              .add(PARAM_CHART_TYPE, chartType)
                              .add(PARAM_AUTHENTICATED, authenticated);

                    return new DefaultImagePlaceholder(jiraChart.getImagePlaceholderUrl(parameters, urlBuilder), null, false);
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
}
