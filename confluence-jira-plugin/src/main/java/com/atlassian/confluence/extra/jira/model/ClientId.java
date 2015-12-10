package com.atlassian.confluence.extra.jira.model;

import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

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

    private ClientId(String serverId, String pageId, String userId, String jqlQuery)
    {
        this.serverId = serverId;
        this.pageId = pageId;
        this.userId = userId;
        this.jqlQuery = jqlQuery;
    }

    public static ClientId fromElement(String serverId, String pageId, String userId, String jqlQuery)
    {
        if (StringUtils.isEmpty(serverId) || StringUtils.isEmpty(pageId) || StringUtils.isEmpty(userId))
        {
            throw new IllegalArgumentException("Wrong ClientId data");
        }
        return new ClientId(serverId, pageId, userId, jqlQuery);
    }

    public static ClientId fromElement(String serverId, String pageId, String userId)
    {
        return fromElement(serverId, pageId, userId, null);
    }

    public static ClientId fromClientId(String clientId)
    {
        String[] elements = clientId.split(SEPARATOR);
        if (elements.length < 3 || elements.length > 4)
        {
            throw new IllegalArgumentException("Wrong clientId format=" + clientId);
        }
        if (elements.length == 3)
        {
            return new ClientId(elements[0], elements[1], elements[2], null);
        }
        else
        {
            return new ClientId(elements[0], elements[1], elements[2], new String(Base64.decodeBase64(elements[3])));
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

    @Override
    public int hashCode()
    {
        int result = pageId != null ? pageId.hashCode() : 0;
        result = 31 * result + (serverId != null ? serverId.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (jqlQuery != null ? jqlQuery.hashCode() : 0);
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
        return true;
    }
}
