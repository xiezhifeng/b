package com.atlassian.confluence.extra.jira.executor;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A callable that executes a streamable macro in the current user context
 */
public class StreamableMacroFutureTask implements Callable<String>
{
    private final Map<String, String> parameters;
    private final ConversionContext context;
    private final StreamableMacro macro;
    private final ConfluenceUser user;
    private final Element element;
    private final String jiraServerUrl;
    private final MacroExecutionException macroExecutionException;

    private static final String ICON_URL = "iconUrl";
    private static final String TEMPLATE_PATH = "templates/extra/jira";
    private static final String SHOW_SUMMARY = "showSummary";

    public StreamableMacroFutureTask(Map<String, String> parameters, ConversionContext context, StreamableMacro macro, ConfluenceUser user)
    {
        this.parameters = parameters;
        this.context = context;
        this.macro = macro;
        this.user = user;
        this.element = null;
        this.jiraServerUrl = null;
        this.macroExecutionException = null;
    }

    public StreamableMacroFutureTask(Map<String, String> parameters, ConversionContext context, StreamableMacro macro, ConfluenceUser user, Element element, String jiraServerUrl, MacroExecutionException macroExecutionException)
    {
        this.parameters = parameters;
        this.context = context;
        this.macro = macro;
        this.user = user;
        this.element = element;
        this.jiraServerUrl = jiraServerUrl;
        this.macroExecutionException = macroExecutionException;
    }

    // MacroExecutionException should be automatically handled by the marshaling chain
    public String call() throws MacroExecutionException
    {
        try
        {
            AuthenticatedUserThreadLocal.set(user);
            String key = parameters.get("key");
            String serverId = parameters.get("serverId");
            if (key != null && serverId != null) // is single issue jira markup
            {
                if (element!= null) {
                   Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
                    String showSummaryParam = JiraUtil.getParamValue(parameters, SHOW_SUMMARY, JiraUtil.SUMMARY_PARAM_POSITION);
                    if (StringUtils.isEmpty(showSummaryParam)) {
                        contextMap.put(SHOW_SUMMARY, true);
                    } else {
                        contextMap.put(SHOW_SUMMARY, Boolean.parseBoolean(showSummaryParam));
                    }
                   return render(contextMap, key, element, jiraServerUrl);
                }
                else {
                    throw macroExecutionException; // exception thrown for the whole batch
                }
            }
            return macro.execute(parameters, null, context);
        }
        finally
        {
            AuthenticatedUserThreadLocal.reset();
        }
    }

    // render the content of the JDOM Element got from the SingleJiraIssuesMapThreadLocal
    private String render(Map<String, Object> contextMap, String key, Element issue, String serverUrl)
    {
        Element resolution = issue.getChild("resolution");
        Element status = issue.getChild("status");

        contextMap.put("resolved", resolution != null && !"-1".equals(resolution.getAttributeValue("id")));
        contextMap.put(ICON_URL, issue.getChild("type").getAttributeValue(ICON_URL));
        contextMap.put("key", key);
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
        contextMap.put("clickableUrl", serverUrl + key);
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticsinglejiraissue.vm", contextMap);
    }
}