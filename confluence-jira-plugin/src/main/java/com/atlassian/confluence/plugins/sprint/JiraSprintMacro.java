package com.atlassian.confluence.plugins.sprint;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.exception.JiraIssueMacroException;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.confluence.plugins.sprint.services.JiraAgileService;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * The macro to display Jira Sprint
 *
 */
public class JiraSprintMacro implements Macro, EditorImagePlaceholder
{
    public static final String JIRASPRINT = "jirasprint";
    private static final String MACRO_RESOURCE_PATH = "/download/resources/confluence.extra.jira:jirasprint-xhtml";
    public static final String MACRO_ID_PARAMETER = "sprintId";

    private final ApplicationLinkResolver applicationLinkResolver;
    private final JiraExceptionHelper jiraExceptionHelper;
    private final ImagePlaceHolderHelper imagePlaceHolderHelper;
    private final JiraAgileService jiraAgileService;
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final I18nResolver i18nResolver;

    /**
     * JiraChartMacro constructor
     *
     * @param applicationLinkResolver applink service to get applink
     * @param jiraExceptionHelper handle exception for macro
     * @param imagePlaceHolderHelper image placeholder helper
     * @param jiraAgileService jira agile service
     */
    public JiraSprintMacro(ApplicationLinkResolver applicationLinkResolver,JiraExceptionHelper jiraExceptionHelper, ImagePlaceHolderHelper imagePlaceHolderHelper,
                           JiraAgileService jiraAgileService, SoyTemplateRenderer soyTemplateRenderer, I18nResolver i18nResolver)
    {
        this.applicationLinkResolver = applicationLinkResolver;
        this.jiraExceptionHelper = jiraExceptionHelper;
        this.imagePlaceHolderHelper = imagePlaceHolderHelper;
        this.jiraAgileService = jiraAgileService;
        this.soyTemplateRenderer = soyTemplateRenderer;
        this.i18nResolver = i18nResolver;
    }

    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context) throws MacroExecutionException
    {
        ApplicationLink applicationLink = applicationLinkResolver.getAppLinkForServer("", parameters.get("serverId"));
        Map<String, Object> contextMap =  MacroUtils.defaultVelocityContext();
        String sprintId = parameters.get("sprintId");
        contextMap.put("sprintId", sprintId);
        try
        {
            if (applicationLink == null)
            {
                throw new MacroExecutionException(i18nResolver.getText("jira.sprint.error.noapplinks"));
            }
            contextMap.put("clickableUrl", generateJiraSprintLink(applicationLink, sprintId, parameters.get("boardId")));
            try
            {
                JiraSprintModel jiraSprintModel = jiraAgileService.getJiraSprint(applicationLink, parameters.get(MACRO_ID_PARAMETER));
                contextMap.put("sprintName", jiraSprintModel.getName());
                contextMap.put("status", jiraSprintModel.getState());
            }
            catch (CredentialsRequiredException credentialsRequiredException)
            {
                contextMap.put("sprintName", getDefaultSprintName(parameters));
                contextMap.put("oAuthUrl", credentialsRequiredException.getAuthorisationURI().toString());
            }

            return soyTemplateRenderer.render("confluence.extra.jira:jirasprint-resources",
                    "Confluence.Templates.ConfluenceJiraPlugin.createSprintMacro", contextMap);
        }
        catch (Exception e)
        {
            contextMap.put("jiraLinkText", getDefaultSprintName(parameters));
            return jiraExceptionHelper.renderNormalJIMExceptionMessage(new JiraIssueMacroException(e, contextMap));
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
        String macroTemplate = String.format("{jirasprint:sprintId=%s}", getDefaultSprintName(parameters));
        return imagePlaceHolderHelper.getMacroImagePlaceholder(macroTemplate, MACRO_RESOURCE_PATH);
    }

    private String getDefaultSprintName(Map<String, String> parameters)
    {
        return StringUtils.defaultString(parameters.get("sprintName"), i18nResolver.getText("confluence.extra.jira.jirasprint.label"));
    }

    private String generateJiraSprintLink(ApplicationLink applicationLink, String sprintId, String boardId)
    {
        return applicationLink.getDisplayUrl() + String.format("/secure/GHLocateSprintOnBoard.jspa?rapidViewId=%s&sprintId=%s", boardId, sprintId);
    }
}
