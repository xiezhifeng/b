package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.opensymphony.util.TextUtils;

import java.util.Map;

/**
 * Action used to allow the user to define the icon image filenames for any custom
 * statuses, priorites or issues types they might have in the JIRA instances they are pulling feeds from
 * using the {jiraissues} macro
 */
public class BrowseJiraIssuesMacroIconMappingsAction extends ConfluenceActionSupport
{
    private JiraIconMappingManager jiraIconMappingManager;

    private String jiraEntityName;
    private String iconFilename;

    public String getActionName(String fullClassName)
    {
        return getText("com.atlassian.confluence.extra.jira.BrowseJiraIssuesMacroIconMappingsAction.action.name");
    }

    public void validate()
    {
        super.validate();

        if (!TextUtils.stringSet(jiraEntityName))
            addActionError(getText("error.jira.entity.name.reqd"));
    }

    public String doAddIconMapping()
    {
        jiraIconMappingManager.addIconMapping(getJiraEntityName(), getIconFilename());
        return SUCCESS;
    }

    public Map getIconMappings()
    {
        return jiraIconMappingManager.getIconMappings();
    }

    public String getJiraEntityName()
    {
        return jiraEntityName;
    }

    public void setJiraEntityName(String jiraEntityName)
    {
        this.jiraEntityName = jiraEntityName;
    }

    public String getIconFilename()
    {
        return iconFilename;
    }

    public void setIconFilename(String iconFilename)
    {
        this.iconFilename = iconFilename;
    }

    public void setJiraIconMappingManager(JiraIconMappingManager jiraIconMappingManager)
    {
        this.jiraIconMappingManager = jiraIconMappingManager;
    }
}