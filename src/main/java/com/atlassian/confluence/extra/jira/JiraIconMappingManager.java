package com.atlassian.confluence.extra.jira;

import com.atlassian.bandana.BandanaManager;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaContext;
import com.atlassian.confluence.setup.bandana.ConfluenceBandanaKeys;
import com.opensymphony.util.TextUtils;

import java.util.Map;
import java.util.HashMap;

/**
 * maintains a mapping of:<br>
 * <ul>
 * <li> priority --> file name of image
 * <li> workflow statuses --> file name of image
 * <li> issue types --> file name of image
 * </ul>
 */
public class JiraIconMappingManager
{
    BandanaManager bandanaManager;

    public Map getIconMappings()
    {
        Map iconMappings = (Map) bandanaManager.getValue(new ConfluenceBandanaContext(), ConfluenceBandanaKeys.JIRA_ICON_MAPPINGS);
        if (iconMappings == null)
            iconMappings = getDefaults();
        return iconMappings;
    }

    public void addIconMapping(String jiraEntity, String iconFileName)
    {
        if (!TextUtils.stringSet(jiraEntity))
            return;

        Map iconMappings = getIconMappings();
        iconMappings.put(jiraEntity, iconFileName);
        updateIconMappings(iconMappings);
    }

    public void removeIconMapping(String jiraEntity)
    {
        Map iconMappings = getIconMappings();
        iconMappings.remove(jiraEntity);
        updateIconMappings(iconMappings);
    }

    public void updateIconMappings(Map iconMappings)
    {
        bandanaManager.setValue(new ConfluenceBandanaContext(), ConfluenceBandanaKeys.JIRA_ICON_MAPPINGS, iconMappings);
    }

    public void setBandanaManager(BandanaManager bandanaManager)
    {
        this.bandanaManager = bandanaManager;
    }

    public boolean hasIconMapping(String jiraEntity)
    {
        if (TextUtils.stringSet(jiraEntity))
            return getIconMappings().containsKey(jiraEntity);
        else
            return false;
    }

    public String getIconFileName(String jiraEntity)
    {
        if (TextUtils.stringSet(jiraEntity))
            return (String) getIconMappings().get(jiraEntity);
        else
            return null;
    }

    private Map getDefaults()
    {
        Map map = new HashMap();

        map.put("Bug", "bug.gif");
        map.put("New Feature", "newfeature.gif");
        map.put("Task", "task.gif");
        map.put("Sub-task", "issue_subtask.gif");
        map.put("Improvement", "improvement.gif");

        map.put("Blocker", "priority_blocker.gif");
        map.put("Critical", "priority_critical.gif");
        map.put("Major", "priority_major.gif");
        map.put("Minor", "priority_minor.gif");
        map.put("Trivial", "priority_trivial.gif");

        map.put("Assigned", "status_assigned.gif");
        map.put("Closed", "status_closed.gif");
        map.put("In Progress", "status_inprogress.gif");
        map.put("Need Info", "status_needinfo.gif");
        map.put("Open", "status_open.gif");
        map.put("Reopened", "status_reopened.gif");
        map.put("Resolved", "status_resolved.gif");
        map.put("Unassigned", "status_unassigned.gif");

        return map;
    }
}