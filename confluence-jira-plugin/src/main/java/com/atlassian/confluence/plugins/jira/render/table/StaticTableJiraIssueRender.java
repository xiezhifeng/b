package com.atlassian.confluence.plugins.jira.render.table;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.definition.RichTextMacroBody;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.renderer.RenderContext;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StaticTableJiraIssueRender extends TableJiraIssueRender
{

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticTableJiraIssueRender.class);

    private static final String ENABLE_REFRESH = "enableRefresh";
    private static final String TOTAL_ISSUES = "totalIssues";
    private static final String JIRA_SERVER_URL = "jiraServerUrl";

    private JiraIssuesDateFormatter jiraIssuesDateFormatter;
    private JiraCacheManager jiraCacheManager;
    private FormatSettingsManager formatSettingsManager;
    private MacroMarshallingFactory macroMarshallingFactory;
    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    @Override
    public void populateSpecifyMacroType(Map<String, Object> contextMap, List<String> columnNames, String url, ApplicationLink appLink, boolean forceAnonymous,
                                         boolean useCache, ConversionContext conversionContext, JiraRequestData jiraRequestData, Map<String, String> params) throws MacroExecutionException
    {
        contextMap.put("singleIssueTable", JiraJqlHelper.isJqlKeyType(jiraRequestData.getRequestData()));
        boolean clearCache = getBooleanProperty(conversionContext.getProperty(DefaultJiraCacheManager.PARAM_CLEAR_CACHE));
        try
        {
            if (RenderContext.DISPLAY.equals(conversionContext.getOutputType()) ||
                    RenderContext.PREVIEW.equals(conversionContext.getOutputType()))
            {
                contextMap.put(ENABLE_REFRESH, Boolean.TRUE);
            }
            if (StringUtils.isNotBlank(conversionContext.getPropertyAsString("orderColumnName")) && StringUtils.isNotBlank(conversionContext.getPropertyAsString("order")))
            {
                contextMap.put("orderColumnName", conversionContext.getProperty("orderColumnName"));
                contextMap.put("order", conversionContext.getProperty("order"));
            }
            if (clearCache)
            {
                jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, forceAnonymous, false);
            }

            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink, forceAnonymous, useCache);
            setupContextMapForStaticTable(contextMap, channel, appLink);
        }
        catch (CredentialsRequiredException e)
        {
            if (clearCache)
            {
                jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, forceAnonymous, true);
            }
            populateContextMapForStaticTableByAnonymous(contextMap, columnNames, url, appLink, forceAnonymous, useCache);
            contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
        }
        catch (MalformedRequestException e)
        {
            LOGGER.info("Can't get issues because issues key is not exist or user doesn't have permission to view", e);
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
        catch (Exception e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }

        setupRefreshLink(contextMap, conversionContext, params);
    }

    @Override
    public String getTemplate(Map<String, Object> contextMap) {
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticJiraIssues.vm", contextMap);
    }

    private void populateContextMapForStaticTableByAnonymous(Map<String, Object> contextMap, List<String> columnNames,
                                                             String url, ApplicationLink appLink, boolean forceAnonymous, boolean useCache)
            throws MacroExecutionException
    {
        try
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(url, columnNames,
                    appLink, forceAnonymous, useCache);
            setupContextMapForStaticTable(contextMap, channel, appLink);
        }
        catch (Exception e)
        {
            // issue/CONFDEV-21600: 'refresh' link should be shown for all cases
            // However, it will be visible if and only if totalIssues has a value
            contextMap.put(TOTAL_ISSUES, 0);
            LOGGER.info("Can't get jira issues by anonymous user from : "+ appLink);
            LOGGER.debug("More info", e);
        }
    }

    private void setupRefreshLink(Map<String, Object> contextMap, ConversionContext conversionContext, Map<String, String> params) throws MacroExecutionException
    {
        int refreshId = getNextRefreshId();

        contextMap.put("refreshId", refreshId);
        MacroDefinition macroDefinition = new MacroDefinition("jira", new RichTextMacroBody(""), null, params);
        try
        {
            Streamable out = macroMarshallingFactory.getStorageMarshaller().marshal(macroDefinition, conversionContext);
            StringWriter writer = new StringWriter();
            out.writeTo(writer);
            contextMap.put("wikiMarkup", writer.toString());
        }
        catch (XhtmlException e)
        {
            throw new MacroExecutionException("Unable to constract macro definition.", e);
        }
        catch (IOException e)
        {
            throw new MacroExecutionException("Unable to constract macro definition.", e);
        }
        // Fix issue/CONF-31836: Jira Issues macro displays java.lang.NullPointerException when included on Welcome Message
        // The reason is that the renderContext used in the Welcome Page is not an instance of PageContext
        // Therefore, conversionContext.getEntity() always returns a null value. to fix this, we need to check if this entity is null or not
        String contentId = conversionContext.getEntity() != null ? conversionContext.getEntity().getIdAsString() : "-1";
        contextMap.put("contentId", contentId);
    }

    private void setupContextMapForStaticTable(Map<String, Object> contextMap, JiraIssuesManager.Channel channel, ApplicationLink appLink)
    {
        Element element = channel.getChannelElement();
        contextMap.put("trustedConnection", channel.isTrustedConnection());
        contextMap.put("trustedConnectionStatus", channel.getTrustedConnectionStatus());
        contextMap.put("channel", element);
        contextMap.put("entries", element.getChildren("item"));
        JiraUtil.checkAndCorrectDisplayUrl(element.getChildren(JiraIssuesMacro.ITEM), appLink);
        try
        {
            Element issue = element.getChild("issue");
            if(issue != null && issue.getAttribute("total") != null)
            {
                contextMap.put(TOTAL_ISSUES, issue.getAttribute("total").getIntValue());
            }
        }
        catch (DataConversionException e)
        {
            contextMap.put(TOTAL_ISSUES, element.getChildren("item").size());
        }
        contextMap.put("xmlXformer", xmlXformer);
        contextMap.put("jiraIssuesManager", jiraIssuesManager);
        contextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
        contextMap.put("jiraIssuesDateFormatter", jiraIssuesDateFormatter);
        contextMap.put("userLocale", getUserLocale(element.getChildText("language")));
        if (null != appLink)
        {
            contextMap.put(JIRA_SERVER_URL, JiraUtil.normalizeUrl(appLink.getDisplayUrl()));
        }
        else
        {
            try
            {
                URL sourceUrl = new URL(channel.getSourceUrl());
                String jiraServerUrl = sourceUrl.getProtocol() + "://" + sourceUrl.getAuthority();
                contextMap.put(JIRA_SERVER_URL, jiraServerUrl);
            }
            catch (MalformedURLException e)
            {
                LOGGER.debug("MalformedURLException thrown when retrieving sourceURL from the channel", e);
                LOGGER.info("Set jiraServerUrl to empty string");
                contextMap.put(JIRA_SERVER_URL, "");
            }
        }

        Locale locale = localeManager.getLocale(AuthenticatedUserThreadLocal.get());
        contextMap.put("dateFormat", new SimpleDateFormat(formatSettingsManager.getDateFormat(), locale));
    }

    private boolean getBooleanProperty(Object value)
    {
        if (value instanceof Boolean)
        {
            return (Boolean) value;
        }
        else if (value instanceof String)
        {
            return BooleanUtils.toBoolean((String) value);
        }
        return false;
    }

    private Locale getUserLocale(String language)
    {
        if (StringUtils.isNotEmpty(language))
        {
            if (language.contains("-"))
            {
                return new Locale(language.substring(0, 2), language.substring(language.indexOf('-') + 1));
            }
            else
            {
                return new Locale(language);// Just the language code only
            }
        }
        return Locale.getDefault();
    }

    private int getNextRefreshId()
    {
        return RandomUtils.nextInt();
    }

    public void setJiraIssuesDateFormatter(JiraIssuesDateFormatter jiraIssuesDateFormatter) {
        this.jiraIssuesDateFormatter = jiraIssuesDateFormatter;
    }

    public void setJiraCacheManager(JiraCacheManager jiraCacheManager) {
        this.jiraCacheManager = jiraCacheManager;
    }

    public void setFormatSettingsManager(FormatSettingsManager formatSettingsManager) {
        this.formatSettingsManager = formatSettingsManager;
    }

    public void setMacroMarshallingFactory(MacroMarshallingFactory macroMarshallingFactory) {
        this.macroMarshallingFactory = macroMarshallingFactory;
    }

    public JiraIssuesXmlTransformer getXmlXformer()
    {
        return xmlXformer;
    }
}
