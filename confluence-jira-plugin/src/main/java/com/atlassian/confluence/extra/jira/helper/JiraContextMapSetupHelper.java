package com.atlassian.confluence.extra.jira.helper;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.SeraphUtils;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import org.apache.commons.lang.StringUtils;

public class JiraContextMapSetupHelper
{
    public static void setupForceAnonymousAndUseCache(JiraRequestData jiraRequestData, ApplicationLink applink)
    {
        String anonymousStr = JiraUtil.getParamValue(jiraRequestData.getParameters(), JiraIssuesMacro.ANONYMOUS, JiraUtil.PARAM_POSITION_4);
        if ("".equals(anonymousStr))
        {
            anonymousStr = "false";
        }
        boolean forceAnonymous = Boolean.valueOf(anonymousStr)
                || (jiraRequestData.getRequestType() == JiraIssuesMacro.Type.URL && SeraphUtils.isUserNamePasswordProvided(jiraRequestData.getRequestData()));

        // support rendering macros which were created without applink by legacy macro
        if (applink == null)
        {
            forceAnonymous = true;
        }
        jiraRequestData.setForceAnonymous(forceAnonymous);


        String cacheParameter = JiraUtil.getParamValue(jiraRequestData.getParameters(), JiraIssuesMacro.CACHE, JiraUtil.PARAM_POSITION_2);
        boolean userAuthenticated = AuthenticatedUserThreadLocal.get() != null;
        if (JiraIssuesMacro.JiraIssuesType.TABLE.equals(jiraRequestData.getIssuesType()) && !JiraJqlHelper.isJqlKeyType(jiraRequestData.getRequestData()))
        {
            jiraRequestData.setUseCache(StringUtils.isBlank(cacheParameter)
                    || cacheParameter.equals("on")
                    || Boolean.valueOf(cacheParameter));
        } else {
            jiraRequestData.setUseCache(userAuthenticated ? forceAnonymous : true); // always cache single issue and count if user is not authenticated);
        }
    }

    public static void setupURL(JiraRequestData jiraRequestData, ApplicationLink applink, int maximumIssues, I18NBean i18NBean, JiraIssuesManager jiraIssuesManager)
            throws MacroExecutionException
    {
        String url = null;
        if (applink != null)
        {
            url = JiraJqlHelper.getXmlUrl(maximumIssues, jiraRequestData, jiraIssuesManager, i18NBean, applink);
        }
        else if (jiraRequestData.getRequestType() == JiraIssuesMacro.Type.URL)
        {
            url = jiraRequestData.getRequestData();
        }

        // support querying with 'no applink' ONLY IF we have base url
        if (url == null && applink == null)
        {
            throw new MacroExecutionException(i18NBean.getText("jiraissues.error.noapplinks"));
        }

        jiraRequestData.setUrl(url);
    }
}
