package it.com.atlassian.confluence.plugins.webdriver.model;

import java.util.HashMap;
import java.util.Map;

public class JiraProjectModel
{
    private String projectKey;
    private String projectName;
    private String projectId;
    private Map<String, String> projectIssueTypes = new HashMap<String, String>();
    private Map<String, String> projectEpicProperties = new HashMap<String, String>();

    public JiraProjectModel(String projectName, String projectKey)
    {
        this.projectName=projectName;
        this.projectKey=projectKey;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setProjectKey(String projectKey)
    {
        this.projectKey = projectKey;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public void setProjectId(String projectId)
    {
        this.projectId = projectId;
    }

    public Map<String, String> getProjectIssueTypes()
    {
        return projectIssueTypes;
    }

    public void setProjectIssueTypes(Map<String, String> projectIssueTypes)
    {
        this.projectIssueTypes = projectIssueTypes;
    }

    public Map<String, String> getProjectEpicProperties()
    {
        return projectEpicProperties;
    }

    public void setProjectEpicProperties(Map<String, String> projectEpicProperties)
    {
        this.projectEpicProperties = projectEpicProperties;
    }
}
