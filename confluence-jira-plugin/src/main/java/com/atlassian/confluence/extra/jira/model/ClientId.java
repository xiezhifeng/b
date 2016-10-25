package com.atlassian.confluence.extra.jira.model;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Generate clientId
 */
public class ClientId
{
    private static final String SEPARATOR = "_";

    private String serverId;
    private String pageId;
    private String userId;
    private String jqlQuery;
    private JiraIssuesType jiraIssuesType;
    private String columnNames;

    private ClientId(JiraIssuesType jiraIssuesType, String serverId, String pageId, String userId, String jqlQuery, String columnNames)
    {
        this.serverId = serverId;
        this.pageId = pageId;
        this.userId = userId;
        this.jqlQuery = jqlQuery;
        this.jiraIssuesType = jiraIssuesType;
        this.columnNames = columnNames;
    }

    public static ClientId fromElement(JiraIssuesType jiraIssuesType, String serverId, String pageId, String userId, String jqlQuery, String columnNames)
    {
        if (StringUtils.isEmpty(serverId) || StringUtils.isEmpty(pageId) || StringUtils.isEmpty(userId))
        {
            throw new IllegalArgumentException("Wrong ClientId data");
        }
        return new ClientId(jiraIssuesType, serverId, pageId, userId, jqlQuery, columnNames);
    }

    public static ClientId fromElement(JiraIssuesType jiraIssuesType, String serverId, String pageId, String userId)
    {
        return fromElement(jiraIssuesType, serverId, pageId, userId, null, null);
    }

    public static ClientId fromClientId(String clientId)
    {
        String[] elements = clientId.split(SEPARATOR);
        if (elements.length == 4)
        {
            return new ClientId(JiraIssuesType.valueOf(elements[0]), elements[1], elements[2], elements[3], null, null);
        }
        else if (elements.length == 5)
        {
            return new ClientId(JiraIssuesType.valueOf(elements[0]), elements[1], elements[2], elements[3], new String(Base64.decodeBase64(elements[4])), null);
        }
        else if (elements.length == 6)
        {
            return new ClientId(JiraIssuesType.valueOf(elements[0]), elements[1], elements[2], elements[3], new String(Base64.decodeBase64(elements[4])), elements[5]);
        }
        throw new IllegalArgumentException("Wrong clientId format=" + clientId);
    }

    public String getServerId()
    {
        return serverId;
    }

    public String getPageId()
    {
        return pageId;
    }

    public String getUserId()
    {
        return userId;
    }

    public String getJqlQuery()
    {
        return jqlQuery;
    }

    public String getColumnNames()
    {
        return columnNames;
    }

    public JiraIssuesType getJiraIssuesType()
    {
        return jiraIssuesType;
    }

    public String toString()
    {
        List<String> params = Lists.newArrayList(jiraIssuesType.toString(), serverId, pageId, userId);
        if (StringUtils.isNotEmpty(jqlQuery))
        {
            params.add(Base64.encodeBase64String(jqlQuery.getBytes()));
        }
        if (StringUtils.isNotEmpty(columnNames))
        {
            params.add(columnNames);
        }
        return StringUtils.join(params, SEPARATOR);
    }

    @Override
    public int hashCode()
    {
        int result = pageId != null ? pageId.hashCode() : 0;
        result = 31 * result + (serverId != null ? serverId.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (jqlQuery != null ? jqlQuery.hashCode() : 0);
        result = 31 * result + (jiraIssuesType != null ? jiraIssuesType.hashCode() : 0);
        result = 31 * result + (columnNames != null ? columnNames.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        ClientId that = (ClientId) o;

        if (!StringUtils.equals(this.serverId, that.serverId))
        {
            return false;
        }
        if (!StringUtils.equals(this.pageId, that.pageId))
        {
            return false;
        }

        if (!StringUtils.equals(this.userId, that.userId))
        {
            return false;
        }

        if (!StringUtils.equals(this.jqlQuery, that.jqlQuery))
        {
            return false;
        }

        if (!StringUtils.equals(this.columnNames, that.columnNames))
        {
            return false;
        }

        if (this.jiraIssuesType != that.jiraIssuesType)
        {
            return false;
        }
        return true;
    }
}
