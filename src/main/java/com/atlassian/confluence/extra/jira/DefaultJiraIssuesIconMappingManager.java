//package com.atlassian.confluence.extra.jira;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//
//public class DefaultJiraIssuesIconMappingManager implements JiraIssuesIconMappingManager
//{
//    private static final Map<String, String> DEFAULT_ICON_MAPPING;
//
//    static
//    {
//        Map<String, String> mutableIconMapping = new HashMap<String, String>();
//
//        mutableIconMapping.put("Bug", "bug.gif");
//        mutableIconMapping.put("New Feature", "newfeature.gif");
//        mutableIconMapping.put("Task", "task.gif");
//        mutableIconMapping.put("Sub-task", "issue_subtask.gif");
//        mutableIconMapping.put("Improvement", "improvement.gif");
//
//        mutableIconMapping.put("Blocker", "priority_blocker.gif");
//        mutableIconMapping.put("Critical", "priority_critical.gif");
//        mutableIconMapping.put("Major", "priority_major.gif");
//        mutableIconMapping.put("Minor", "priority_minor.gif");
//        mutableIconMapping.put("Trivial", "priority_trivial.gif");
//
//        mutableIconMapping.put("Assigned", "status_assigned.gif");
//        mutableIconMapping.put("Closed", "status_closed.gif");
//        mutableIconMapping.put("In Progress", "status_inprogress.gif");
//        mutableIconMapping.put("Need Info", "status_needinfo.gif");
//        mutableIconMapping.put("Open", "status_open.gif");
//        mutableIconMapping.put("Reopened", "status_reopened.gif");
//        mutableIconMapping.put("Resolved", "status_resolved.gif");
//        mutableIconMapping.put("Unassigned", "status_unassigned.gif");
//
//        DEFAULT_ICON_MAPPING = Collections.unmodifiableMap(mutableIconMapping);
//    }
//
//    private final JiraIssuesSettingsManager jiraIssuesSettingsManager;
//
//    public DefaultJiraIssuesIconMappingManager(JiraIssuesSettingsManager jiraIssuesSettingsManager)
//    {
//        this.jiraIssuesSettingsManager = jiraIssuesSettingsManager;
//    }
//
//    public Map<String, String> getBaseIconMapping()
//    {
//        Map<String, String> iconMapping = jiraIssuesSettingsManager.getIconMapping();
//        return null == iconMapping ? DEFAULT_ICON_MAPPING : iconMapping;
//    }
//
//    public Map<String, String> getFullIconMapping(String link)
//    {
//        // In pre 3.7 JIRA, the link is just http://domain/context, in 3.7 and later it is the full query URL,
//        // which looks like http://domain/context/secure/IssueNaviagtor...
//        int index = link.indexOf("/secure/IssueNavigator");
//        if (index != -1)
//            link = link.substring(0, index);
//
//        StringBuilder stringBuilder = new StringBuilder();
//        String imagesRoot = stringBuilder.append(link).append("/images/icons/").toString();
//        Map<String, String> baseIconMapping = getBaseIconMapping();
//        Map<String, String> result = new HashMap<String, String>(baseIconMapping);
//
//        for (Map.Entry<String, String> entry : baseIconMapping.entrySet())
//        {
//            String iconFileName = entry.getValue();
//            if (iconFileName.startsWith("http://") || iconFileName.startsWith("https://"))
//                result.put(entry.getKey(), iconFileName);
//            else
//            {
//                stringBuilder.setLength(0);
//                result.put(entry.getKey(), stringBuilder.append(imagesRoot).append(iconFileName).toString());
//            }
//        }
//
//        return result;
//    }
//
//    public void addBaseIconMapping(String issueType, String iconFileName)
//    {
//        Map<String, String> iconMapping = new HashMap<String, String>(getBaseIconMapping());
//
//        iconMapping.put(issueType, iconFileName);
//        jiraIssuesSettingsManager.setIconMapping(iconMapping);
//    }
//
//    public void removeBaseIconMapping(String issueType)
//    {
//        Map<String, String> iconMapping = new HashMap<String, String>(getBaseIconMapping());
//
//        iconMapping.remove(issueType);
//        jiraIssuesSettingsManager.setIconMapping(iconMapping);
//    }
//
//    public boolean hasBaseIconMapping(String issueType)
//    {
//        return getBaseIconMapping().containsKey(issueType);
//    }
//
//    public String getIconFileName(String issueType)
//    {
//        return getBaseIconMapping().get(issueType);
//    }
//}
