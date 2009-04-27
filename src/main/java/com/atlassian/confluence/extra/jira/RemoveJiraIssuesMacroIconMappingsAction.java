package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoveJiraIssuesMacroIconMappingsAction extends ConfluenceActionSupport
{
    private static final Logger log = Logger.getLogger(RemoveJiraIssuesMacroIconMappingsAction.class);

    private JiraIssuesIconMappingManager jiraIssuesIconMappingManager;

    private List<String> entitiesToRemove = new ArrayList<String>();

    public JiraIssuesIconMappingManager getJiraIssuesIconMappingManager()
    {
        return jiraIssuesIconMappingManager;
    }

    public void setJiraIssuesIconMappingManager(JiraIssuesIconMappingManager jiraIssuesIconMappingManager)
    {
        this.jiraIssuesIconMappingManager = jiraIssuesIconMappingManager;
    }

    public String execute()
    {
        for (String entity : entitiesToRemove)
        {
            log.info("Removing " + entity);
            jiraIssuesIconMappingManager.removeBaseIconMapping(entity);
        }
        return SUCCESS;
    }

    public void validate()
    {
        super.validate();

        if (entitiesToRemove.isEmpty())
            addActionError(getText("icon.mappings.none.selected"));
    }

    public Map<String, String> getIconMappings()
    {
        return jiraIssuesIconMappingManager.getBaseIconMapping();
    }

    public void setEntitiesToRemove(List<String> entitiesToRemove)
    {
        this.entitiesToRemove.clear();
        this.entitiesToRemove.addAll(entitiesToRemove);
    }
}