package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Action used to allow the user to define the icon image filenames for any custom
 * statuses, priorites or issues types they might have in the JIRA instances they are pulling feeds from
 * using the {jiraissues} macro
 */
public class BrowseJiraIssuesMacroIconMappingsAction extends ConfluenceActionSupport
{
    private JiraIssuesIconMappingManager jiraIssuesIconMappingManager;

    private String jiraEntityName;
    private String iconFilename;

    public void setJiraIssuesIconMappingManager(JiraIssuesIconMappingManager jiraIssuesIconMappingManager)
    {
        this.jiraIssuesIconMappingManager = jiraIssuesIconMappingManager;
    }

    public void validate()
    {
        super.validate();

        if (!StringUtils.isNotBlank(jiraEntityName))
            addActionError(getText("error.jira.entity.name.reqd"));
    }

    public String doAddIconMapping()
    {
        jiraIssuesIconMappingManager.addBaseIconMapping(getJiraEntityName(), getIconFilename());
        return SUCCESS;
    }

    public Map getIconMappings()
    {
        return jiraIssuesIconMappingManager.getBaseIconMapping();
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
}