package com.atlassian.confluence.plugins.jiracharts;

import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_AUTHENTICATED;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_CHART_TYPE;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_JQL;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.PARAM_SERVER_ID;
import static com.atlassian.confluence.plugins.jiracharts.helper.JiraChartHelper.isSupportedChart;

import java.util.Map;
import java.util.concurrent.Future;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChart;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChartFactory;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.UrlBuilder;

import com.atlassian.applinks.api.ApplicationId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The macro to display Jira chart
 *
 */
public class JiraChartMacro implements StreamableMacro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);
    private static final String IMAGE_GENERATOR_SERVLET = "/plugins/servlet/image-generator";
    private static final String TEMPLATE_PATH = "templates/jirachart/";
    private ReadOnlyApplicationLinkService readOnlyApplicationLinkService;

    private JQLValidator jqlValidator;

    private final MacroExecutorService executorService;
    private final I18NBeanFactory i18NBeanFactory;
    private final JiraConnectorManager jiraConnectorManager;
    private final JiraChartFactory jiraChartFactory;
    private final JiraExceptionHelper jiraExceptionHelper;

    /**
     * JiraChartMacro constructor
     *
     * @param executorService executorService
     * @param readOnlyApplicationLinkService applink service to get applink
     * @param i18NBeanFactory I18n bean factory
     */
    public JiraChartMacro(MacroExecutorService executorService, ReadOnlyApplicationLinkService readOnlyApplicationLinkService, I18NBeanFactory i18NBeanFactory,
            JiraConnectorManager jiraConnectorManager, JiraChartFactory jiraChartFactory, JiraExceptionHelper jiraExceptionHelper)
    {
        this.executorService = executorService;
        this.i18NBeanFactory = i18NBeanFactory;
        this.readOnlyApplicationLinkService = readOnlyApplicationLinkService;
        this.jiraConnectorManager = jiraConnectorManager;
        this.jiraChartFactory = jiraChartFactory;
        this.jiraExceptionHelper = jiraExceptionHelper;
    }

    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context)
            throws MacroExecutionException
    {
        String chartType = parameters.get(PARAM_CHART_TYPE);
        if(!isSupportedChart(chartType))
        {
            throw new MacroExecutionException(i18NBeanFactory.getI18NBean().getText("jirachart.error.not.supported"));
        }

        JiraChart jiraChart = jiraChartFactory.getJiraChartRenderer(chartType);

        //TODO: there is a performance issue. we have to check the result first. If it's not valid, we will stop and render a error message
        JQLValidationResult result = getJqlValidator().doValidate(parameters, jiraChart.isVerifyChartSupported());

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

    @SuppressWarnings("deprecation") // for Confluence 5.4 backward compatible
    @Override
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext context)
    {
        return new DefaultImagePlaceholder("/download/resources/confluence.extra.jira/jira-table.png", null, false);
    }

    @Override
    public Streamable executeToStream(Map<String, String> parameters, Streamable body, ConversionContext context)
            throws MacroExecutionException
    {
        Future<String> futureResult = executorService.submit(new StreamableMacroFutureTask(jiraExceptionHelper, parameters, context, this,
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
            this.setJqlValidator(new DefaultJQLValidator(readOnlyApplicationLinkService, i18NBeanFactory, jiraConnectorManager));
        }
        return jqlValidator;
    }

    public void setJqlValidator(JQLValidator jqlValidator)
    {
        this.jqlValidator = jqlValidator;
    }
}
