package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RemoveJiraIssuesMacroIconMappingsAction extends ConfluenceActionSupport
{
    private static final Logger log = Logger.getLogger(com.atlassian.confluence.extra.jira.RemoveJiraIssuesMacroIconMappingsAction.class);

    private JiraIconMappingManager jiraIconMappingManager;

    private List entitiesToRemove = new ArrayList();

    public String execute()
    {
        for (Iterator iter = entitiesToRemove.iterator(); iter.hasNext();)
        {
            String entity = (String) iter.next();
            log.info("Removing " + entity);
            jiraIconMappingManager.removeIconMapping(entity);
        }
        return SUCCESS;
    }

    public void validate()
    {
        super.validate();

        if (entitiesToRemove.isEmpty())
            addActionError(getText("icon.mappings.none.selected"));
    }

    public Map getIconMappings()
    {
        return jiraIconMappingManager.getIconMappings();
    }

    public void setJiraIconMappingManager(JiraIconMappingManager jiraIconMappingManager)
    {
        this.jiraIconMappingManager = jiraIconMappingManager;
    }

    public void setEntitiesToRemove(List entitiesToRemove)
    {
        this.entitiesToRemove.clear();
        this.entitiesToRemove.addAll(entitiesToRemove);
    }
}