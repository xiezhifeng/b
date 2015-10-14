package com.atlassian.confluence.plugins.sprint;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.sprint.model.JiraSprintModel;
import com.atlassian.confluence.plugins.sprint.services.JiraAgileService;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The macro to display Jira Sprint
 *
 */
public class JiraSprintMacro implements Macro, EditorImagePlaceholder
{
    private static Logger log = LoggerFactory.getLogger(JiraSprintMacro.class);
    private static final String MACRO_RESOURCE_PATH = "/download/resources/confluence.extra.jira:jirasprint-xhtml";
    private static final String TEMPLATE_PATH = "templates/sprint/";
    private static final String MACRO_ID_PARAMETER = "sprintId";

    private final ApplicationLinkResolver applicationLinkResolver;
    private final JiraExceptionHelper jiraExceptionHelper;
    private final ImagePlaceHolderHelper imagePlaceHolderHelper;
    private final JiraAgileService jiraAgileService;

    /**
     * JiraChartMacro constructor
     *
     * @param applicationLinkResolver applink service to get applink
     * @param jiraExceptionHelper handle exception for macro
     * @param imagePlaceHolderHelper image placeholder helper
     * @param jiraAgileService jira agile service
     */
    public JiraSprintMacro(ApplicationLinkResolver applicationLinkResolver,JiraExceptionHelper jiraExceptionHelper, ImagePlaceHolderHelper imagePlaceHolderHelper,
                           JiraAgileService jiraAgileService)
    {
        this.applicationLinkResolver = applicationLinkResolver;
        this.jiraExceptionHelper = jiraExceptionHelper;
        this.imagePlaceHolderHelper = imagePlaceHolderHelper;
        this.jiraAgileService = jiraAgileService;
    }

    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context) throws MacroExecutionException
    {
        ApplicationLink applicationLink = applicationLinkResolver.getAppLinkForServer("", parameters.get("serverId"));
        try
        {
            Map<String, Object> contextMap =  MacroUtils.defaultVelocityContext();
            try
            {
                JiraSprintModel jiraSprintModel = jiraAgileService.getJiraSprint(applicationLink, parameters.get(MACRO_ID_PARAMETER));
                contextMap.put("sprintName", jiraSprintModel.getName());
                contextMap.put("status", jiraSprintModel.getState());
                contextMap.put("clickableUrl", generateBoardLink(applicationLink, parameters.get("boardId"), jiraSprintModel));
            }
            catch (CredentialsRequiredException credentialsRequiredException)
            {
                contextMap.put("sprintName", "Jira Sprint");
                contextMap.put("oAuthUrl", credentialsRequiredException.getAuthorisationURI().toString());
            }
            return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "jirasprint.vm", contextMap);
        }
        catch (Exception e)
        {
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
        String macroTemplate = String.format("{jirasprint:sprintId=%s}", parameters.get(MACRO_ID_PARAMETER));
        return imagePlaceHolderHelper.getMacroImagePlaceholder(macroTemplate, MACRO_RESOURCE_PATH);
    }

    private String generateBoardLink(ApplicationLink applicationLink, String boardId, JiraSprintModel jiraSprintModel)
    {
        String board = StringUtils.defaultIfBlank(boardId, String.valueOf(jiraSprintModel.getOriginBoardId()));
        String rapidBoardUrl = applicationLink.getDisplayUrl() + "/secure/RapidBoard.jspa?rapidView=" + board;
        if (StringUtils.equalsIgnoreCase(jiraSprintModel.getState(), "closed"))
        {
            rapidBoardUrl += "&view=reporting&chart=burndownChart&sprint=" + jiraSprintModel.getId();
        }
        else if (StringUtils.equalsIgnoreCase(jiraSprintModel.getState(), "future"))
        {
            rapidBoardUrl += "&view=planning";
        }
        return rapidBoardUrl;
    }
}
