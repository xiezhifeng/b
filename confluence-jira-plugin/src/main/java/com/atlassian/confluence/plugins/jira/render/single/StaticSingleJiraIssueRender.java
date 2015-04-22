package com.atlassian.confluence.plugins.jira.render.single;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StaticSingleJiraIssueRender extends SingleJiraIssueRender
{

    private static final String ICON_URL = "iconUrl";
    private static final String IS_NOT_PERMISSION_TO_VIEW = "isNoPermissionToView";
    public static final List<String> DEFAULT_COLUMNS_FOR_SINGLE_ISSUE = Arrays.asList("summary", "type", "resolution", "status");

    @Override
    public void populateSpecifyMacroType(Map<String, Object> contextMap, List<String> columnNames, String url, ApplicationLink appLink, boolean forceAnonymous,
                                         boolean useCache, ConversionContext conversionContext, JiraRequestData jiraRequestData, Map<String, String> params) throws MacroExecutionException
    {
        if (RenderContext.EMAIL.equals(conversionContext.getOutputDeviceType())
                || RenderContext.EMAIL.equals(conversionContext.getOutputType()))
        {
            contextMap.put(IS_NOT_PERMISSION_TO_VIEW, true);
        }
        else
        {
            populateContextMapForStaticSingleIssue(contextMap, url, appLink, forceAnonymous, useCache, conversionContext);
        }
    }

    @Override
    public String getTemplate(Map<String, Object> contextMap) {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticsinglejiraissue.vm", contextMap);
    }

    // render a single JIRA issue from a JDOM Element
    public String renderSingleJiraIssue(Map<String, String> parameters, ConversionContext conversionContext, Element issue, String serverUrl) throws Exception {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        String outputType = conversionContext.getOutputType();
        // added parameters for pdf export
        setRenderMode(contextMap, outputType);

        String showSummaryParam = JiraUtil.getParamValue(parameters, JiraIssuesMacro.SHOW_SUMMARY, JiraUtil.SUMMARY_PARAM_POSITION);
        if (StringUtils.isEmpty(showSummaryParam))
        {
            contextMap.put(JiraIssuesMacro.SHOW_SUMMARY, true);
        }
        else
        {
            contextMap.put(JiraIssuesMacro.SHOW_SUMMARY, Boolean.parseBoolean(showSummaryParam));
        }
        setupContextMapForStaticSingleIssue(contextMap, issue, null);
        contextMap.put(JiraIssuesMacro.CLICKABLE_URL, serverUrl + issue.getChild(JiraIssuesMacro.KEY).getValue());

        boolean isMobile = JiraIssuesMacro.MOBILE.equals(conversionContext.getOutputDeviceType());

        if (isMobile)
        {
            return VelocityUtils.getRenderedTemplate(TEMPLATE_MOBILE_PATH + "/mobileSingleJiraIssue.vm", contextMap);
        }
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticsinglejiraissue.vm", contextMap);
    }

    private void setRenderMode(Map<String, Object> contextMap, String outputType)
    {
        if (RenderContext.PDF.equals(outputType))
        {
            contextMap.put(PDF_EXPORT, Boolean.TRUE);
        }
        if (RenderContext.EMAIL.equals(outputType))
        {
            contextMap.put(EMAIL_RENDER, Boolean.TRUE);
        }
    }

    private void populateContextMapForStaticSingleIssue(
            Map<String, Object> contextMap, String url,
            ApplicationLink applicationLink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext)
            throws MacroExecutionException
    {
        JiraIssuesManager.Channel channel;
        try
        {
            channel = jiraIssuesManager.retrieveXMLAsChannel(url, DEFAULT_COLUMNS_FOR_SINGLE_ISSUE, applicationLink,
                    forceAnonymous, useCache);
            setupContextMapForStaticSingleIssue(contextMap, channel.getChannelElement().getChild(JiraIssuesMacro.ITEM), applicationLink);
        }
        catch (CredentialsRequiredException credentialsRequiredException)
        {
            try
            {
                populateContextMapForStaticSingleIssueAnonymous(contextMap, url, applicationLink, forceAnonymous, useCache, conversionContext);
            }
            catch (MacroExecutionException e)
            {
                contextMap.put("oAuthUrl", credentialsRequiredException.getAuthorisationURI().toString());
            }
        }
        catch (MalformedRequestException e)
        {
            contextMap.put(IS_NOT_PERMISSION_TO_VIEW, true);
        }
        catch (Exception e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
    }

    private void populateContextMapForStaticSingleIssueAnonymous(
            Map<String, Object> contextMap, String url,
            ApplicationLink applink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext)
            throws MacroExecutionException
    {
        JiraIssuesManager.Channel channel;
        try
        {
            channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(
                    url, DEFAULT_COLUMNS_FOR_SINGLE_ISSUE, applink, forceAnonymous, useCache);
            setupContextMapForStaticSingleIssue(contextMap, channel.getChannelElement().getChild(JiraIssuesMacro.ITEM), applink);
        }
        catch (Exception e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
    }

    private void setupContextMapForStaticSingleIssue(Map<String, Object> contextMap, Element issue, ApplicationLink applicationLink) throws MalformedRequestException
    {
        //In Jira 6.3, when anonymous make a request to jira without permission, the result will return a empty channel
        if (issue == null && AuthenticatedUserThreadLocal.isAnonymousUser())
        {
            throw new MalformedRequestException();
        }

        Element resolution = issue.getChild("resolution");
        Element status = issue.getChild("status");

        JiraUtil.checkAndCorrectIconURL(issue, applicationLink);

        contextMap.put("resolved", resolution != null && !"-1".equals(resolution.getAttributeValue("id")));
        contextMap.put(ICON_URL, issue.getChild("type").getAttributeValue(ICON_URL));
        String key = issue.getChild(JiraIssuesMacro.KEY).getValue();
        contextMap.put(JiraIssuesMacro.KEY, key);
        contextMap.put("summary", issue.getChild("summary").getValue());
        contextMap.put("status", status.getValue());
        contextMap.put("statusIcon", status.getAttributeValue(ICON_URL));

        Element statusCategory = issue.getChild("statusCategory");
        if (null != statusCategory)
        {
            String colorName = statusCategory.getAttribute("colorName").getValue();
            String keyName = statusCategory.getAttribute("key").getValue();
            if (StringUtils.isNotBlank(colorName) && StringUtils.isNotBlank(keyName))
            {
                contextMap.put("statusColor", colorName);
                contextMap.put("keyName", keyName);
            }
        }
    }

}
