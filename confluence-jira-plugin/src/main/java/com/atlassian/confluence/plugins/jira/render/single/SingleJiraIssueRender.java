package com.atlassian.confluence.plugins.jira.render.single;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRender;
import org.apache.commons.codec.binary.Base64;
import java.util.Map;

public abstract class SingleJiraIssueRender extends JiraIssueRender
{
    private static final String JIRA_ISSUES_RESOURCE_PATH = "jiraissues-xhtml";
    private static final String JIRA_ISSUES_SINGLE_MACRO_TEMPLATE = "{jiraissues:key=%s}";
    private static final String JIRA_SINGLE_MACRO_TEMPLATE = "{jira:key=%s}";
    private static final String JIRA_SINGLE_ISSUE_IMG_SERVLET_PATH_TEMPLATE = "/plugins/servlet/confluence/placeholder/macro?definition=%s&locale=%s";

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
}
