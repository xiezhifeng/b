package com.atlassian.confluence.plugins.jiracharts;

import com.google.common.collect.Collections2;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public enum JiraStatType
{
    STATUES ("statuses", "jirachart.macro.dialog.statistype.statuses"),
    PRIORITIES ("priorities", "jirachart.macro.dialog.statistype.priorities"),
    ASSIGNEES ("assignees", "jirachart.macro.dialog.statistype.assignees"),
    ALL_FIX_FOR ("allFixfor", "jirachart.macro.dialog.statistype.allFixfor"),
    COMPONENTS ("components", "jirachart.macro.dialog.statistype.components"),
    ISSUE_TYPE ("issuetype", "jirachart.macro.dialog.statistype.issuetype");

    private String jiraKey;

    private String resourceKey;

    JiraStatType(String jiraKey, String resourceKey){
        setJiraKey(jiraKey);
        setResourceKey(resourceKey);
    }

    public String getJiraKey()
    {
        return jiraKey;
    }

    private void setJiraKey(String jiraKey)
    {
        this.jiraKey = jiraKey;
    }

    public String getResourceKey()
    {
        return resourceKey;
    }

    private void setResourceKey(String resourceKey)
    {
        this.resourceKey = resourceKey;
    }

    public static JiraStatType getByJiraKey(String key){
        for (JiraStatType element : JiraStatType.values()){
            if (element.getJiraKey().equals(key)){
                return element;
            }
        }

        // just return the default value
        return JiraStatType.STATUES;
    }
}
