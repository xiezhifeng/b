package com.atlassian.confluence.plugins.jira.beans;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JiraIssueBean extends BasicJiraIssueBean
{
    @XmlElement
    private String summary;

    @XmlElement
    private String description;

    @XmlElement
    private String projectId;

    @XmlElement
    private String issueTypeId;

    @XmlElement
    private String reporter;

    @XmlElement
    private String assignee;

    @XmlElement
    private String priority;

    @XmlElement()
    private String error;

    @XmlElement(name="customFields")
    private Map<String, String> customFields = new HashMap<String, String>();
    
    public JiraIssueBean()
    {

    }

    public JiraIssueBean(String projectId, String issueTypeId, String summary, String description)
    {
        this.projectId = projectId;
        this.issueTypeId = issueTypeId;
        this.summary = summary;
        this.description = description;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getProjectId()
    {
        return projectId;
    }

    public void setProjectId(String projectId)
    {
        this.projectId = projectId;
    }

    public String getIssueTypeId()
    {
        return issueTypeId;
    }

    public void setIssueTypeId(String issueTypeId)
    {
        this.issueTypeId = issueTypeId;
    }

    public String getReporter()
    {
        return reporter;
    }

    public void setReporter(String reporter)
    {
        this.reporter = reporter;
    }

    public String getAssignee()
    {
        return assignee;
    }

    public void setAssignee(String assignee)
    {
        this.assignee = assignee;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        this.error = error;
    }

    public Map<String, String> getCustomFields()
    {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields)
    {
        this.customFields = customFields;
    }
    
}
