package com.atlassian.confluence.extra.jira.helper;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import org.apache.commons.lang.StringUtils;

public class JiraContextMapSetupHelper
{

    /*public static boolean isUseCache(JiraIssuesMacro.JiraIssuesType issuesType)
    {
        boolean userAuthenticated = AuthenticatedUserThreadLocal.get() != null;
        if (JiraIssuesMacro.JiraIssuesType.TABLE.equals(issuesType) && !JiraJqlHelper.isJqlKeyType(jiraRequestData.getRequestData()))
        {
            return StringUtils.isBlank(cacheParameter)
                    || cacheParameter.equals("on")
                    || Boolean.valueOf(cacheParameter);
        }

        return userAuthenticated ? forceAnonymous : true; // always cache single issue and count if user is not authenticated
    }*/
}
