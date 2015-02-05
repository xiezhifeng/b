package com.atlassian.confluence.plugins.jira.render;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.renderer.RenderContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SingleJiraIssueRender extends JiraIssueRender {


    private static final String JIRA_ISSUES_RESOURCE_PATH = "jiraissues-xhtml";
    private static final String JIRA_ISSUES_SINGLE_MACRO_TEMPLATE = "{jiraissues:key=%s}";
    private static final String JIRA_SINGLE_MACRO_TEMPLATE = "{jira:key=%s}";
    private static final String JIRA_SINGLE_ISSUE_IMG_SERVLET_PATH_TEMPLATE = "/plugins/servlet/confluence/placeholder/macro?definition=%s&locale=%s";
    private static final String ICON_URL = "iconUrl";
    public static final List<String> DEFAULT_COLUMNS_FOR_SINGLE_ISSUE = Arrays.asList("summary", "type", "resolution", "status");

    @Override
    public ImagePlaceholder getImagePlaceholder(JiraRequestData jiraRequestData, Map<String, String> parameters, String resourcePath) {

        String key = jiraRequestData.getRequestData();
        if (jiraRequestData.getRequestType() == JiraIssuesMacro.Type.URL)
        {
            key = JiraJqlHelper.getKeyFromURL(key);
        }
        return getSingleImagePlaceHolder(key, resourcePath);
    }

    /**
     * Get Image Placeholder of single issue
     * @param key
     * @param resourcePath
     * @return Jira Single Issue Macro Image Placeholder
     */
    private ImagePlaceholder getSingleImagePlaceHolder(String key, String resourcePath)
    {
        String macro = resourcePath.contains(JIRA_ISSUES_RESOURCE_PATH) ?
                String.format(JIRA_ISSUES_SINGLE_MACRO_TEMPLATE, key) : String.format(JIRA_SINGLE_MACRO_TEMPLATE, key);
        byte[] encoded = Base64.encodeBase64(macro.getBytes());
        String locale = localeManager.getSiteDefaultLocale().toString();
        String placeHolderUrl = String.format(JIRA_SINGLE_ISSUE_IMG_SERVLET_PATH_TEMPLATE, new String(encoded), locale);
        return new DefaultImagePlaceholder(placeHolderUrl, null, false);
    }

    @Override
    public void populateSpecifyMacroType(Map<String, Object> contextMap, List<String> columnNames, String url, ApplicationLink appLink, boolean forceAnonymous,
                                         boolean useCache, ConversionContext conversionContext, JiraRequestData jiraRequestData, Map<String, String> params) throws MacroExecutionException
    {
        setKeyInContextMap(jiraRequestData, contextMap);

        if (RenderContext.EMAIL.equals(conversionContext.getOutputDeviceType())
                || RenderContext.EMAIL.equals(conversionContext.getOutputType()))
        {
            contextMap.put(IS_NO_PERMISSION_TO_VIEW, true);
        }
        else
        {
            populateContextMapForStaticSingleIssue(contextMap, url, appLink, forceAnonymous, useCache, conversionContext);
        }
    }

    @Override
    public String getMobileTemplate(Map<String, Object> contextMap) {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_MOBILE_PATH + "/mobileSingleJiraIssue.vm", contextMap);
    }

    @Override
    public String getTemplate(Map<String, Object> contextMap, boolean staticMode) {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + (staticMode ? "/staticsinglejiraissue.vm" : "/dynamicJiraIssues.vm"), contextMap);
    }


    private void setKeyInContextMap(JiraRequestData jiraRequestData, Map<String, Object> contextMap)
    {
        String key = jiraRequestData.getRequestData();
        if(jiraRequestData.getRequestType() == JiraIssuesMacro.Type.URL)
        {
            key = JiraJqlHelper.getKeyFromURL(jiraRequestData.getRequestData());
        }
        contextMap.put(KEY, key);
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
            setupContextMapForStaticSingleIssue(contextMap, channel.getChannelElement().getChild(ITEM), applicationLink);
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
            contextMap.put(IS_NO_PERMISSION_TO_VIEW, true);
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
            setupContextMapForStaticSingleIssue(contextMap, channel.getChannelElement().getChild(ITEM), applink);
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
        String key = issue.getChild(KEY).getValue();
        contextMap.put(KEY, key);
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
