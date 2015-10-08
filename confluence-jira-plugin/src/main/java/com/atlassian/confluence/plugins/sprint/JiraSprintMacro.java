package com.atlassian.confluence.plugins.sprint;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.extra.jira.executor.MacroExecutorService;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.JQLValidator;
import com.atlassian.confluence.plugins.jiracharts.render.JiraChartFactory;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.confluence.plugins.sprint.services.JiraAgileService;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The macro to display Jira chart
 *
 */
public class JiraSprintMacro implements Macro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraSprintMacro.class);
    private static final String IMAGE_GENERATOR_SERVLET = "/plugins/servlet/image-generator";
    private static final String TEMPLATE_PATH = "templates/sprint/";
    private final ApplicationLinkResolver applicationLinkResolver;

    private JQLValidator jqlValidator;

    private final MacroExecutorService executorService;
    private final I18NBeanFactory i18NBeanFactory;
    private final JiraConnectorManager jiraConnectorManager;
    private final JiraChartFactory jiraChartFactory;
    private final JiraExceptionHelper jiraExceptionHelper;
    private final ImagePlaceHolderHelper imagePlaceHolderHelper;
    private final JiraAgileService jiraAgileService;

    /**
     * JiraChartMacro constructor
     *
     * @param executorService executorService
     * @param applicationLinkResolver applink service to get applink
     * @param i18NBeanFactory I18n bean factory
     */
    public JiraSprintMacro(MacroExecutorService executorService, ApplicationLinkResolver applicationLinkResolver, I18NBeanFactory i18NBeanFactory,
                           JiraConnectorManager jiraConnectorManager, JiraChartFactory jiraChartFactory, JiraExceptionHelper jiraExceptionHelper,
                           ImagePlaceHolderHelper imagePlaceHolderHelper, JiraAgileService jiraAgileService)
    {
        this.executorService = executorService;
        this.i18NBeanFactory = i18NBeanFactory;
        this.applicationLinkResolver = applicationLinkResolver;
        this.jiraConnectorManager = jiraConnectorManager;
        this.jiraChartFactory = jiraChartFactory;
        this.jiraExceptionHelper = jiraExceptionHelper;
        this.imagePlaceHolderHelper = imagePlaceHolderHelper;
        this.jiraAgileService = jiraAgileService;
    }

    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context) throws MacroExecutionException
    {
        ApplicationLink applicationLink = applicationLinkResolver.getAppLinkForServer("", parameters.get("serverId"));
        try {
            JiraSprintModel jiraSprintModel = jiraAgileService.getJiraSprint(applicationLink, parameters.get("key"));
            Map<String, Object> contextMap =  MacroUtils.defaultVelocityContext();
            contextMap.put("key", jiraSprintModel.getName());
            contextMap.put("status", jiraSprintModel.getState());
            contextMap.put("clickableUrl", jiraSprintModel.getBoardUrl());
            return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "jirasprint.vm", contextMap);
        } catch (Exception e) {
            return jiraExceptionHelper.renderNormalJIMExceptionMessage(e);
        }
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
        String resourcePath = "/download/resources/confluence.extra.jira:jirasprint-xhtml";
        String macroTemplate = String.format("{jirasprint:key=%s}", parameters.get("key"));
        return imagePlaceHolderHelper.getMacroImagePlaceholder(macroTemplate, resourcePath);
    }
}
