package com.atlassian.confluence.extra.jira.services;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Generate clientId
 */
public class ClientIdGenerator
{
    private static final String SEPARATOR = "_";

    private String serverId;
    private String pageId;
    private String userId;
    private String jqlQuery;

    private ClientIdGenerator(String serverId, String pageId, String userId, String jqlQuery)
    {
        this.serverId = serverId;
        this.pageId = pageId;
        this.userId = userId;
        this.jqlQuery = jqlQuery;
    }

    public static ClientIdGenerator fromElement(String serverId, String pageId, String userId, String jqlQuery)
    {
        if (StringUtils.isAnyEmpty(serverId, pageId, userId))
        {
            throw new IllegalArgumentException("Wrong ClientId data");
        }
        return new ClientIdGenerator(serverId, pageId, userId, jqlQuery);
    }

    public static ClientIdGenerator fromElement(String serverId, String pageId, String userId)
    {
        return fromElement(serverId, pageId, userId, "");
    }

    public static ClientIdGenerator fromClientId(String clientId)
    {
        String[] elements = clientId.split(SEPARATOR);
        if (elements.length < 3 || elements.length > 4)
        {
            throw new IllegalArgumentException("Wrong clientId format=" + clientId);
        }
        if (elements.length == 3)
        {
            return new ClientIdGenerator(elements[0], elements[1], elements[2], null);
        }
        else
        {
            return new ClientIdGenerator(elements[0], elements[1], elements[2], new String(Base64.decodeBase64(elements[3])));
        }
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

    public String toString()
    {
        List<String> params = Lists.newArrayList(serverId, pageId, userId);
        if (StringUtils.isNotEmpty(jqlQuery))
        {
            params.add(Base64.encodeBase64String(jqlQuery.getBytes()));
        }
        return StringUtils.join(params, SEPARATOR);
    }
}
