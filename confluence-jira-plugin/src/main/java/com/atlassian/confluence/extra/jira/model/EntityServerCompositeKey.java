package com.atlassian.confluence.extra.jira.model;

/**
 * Composite key for server Id and Entity Id
 */
public class EntityServerCompositeKey
{
    private final String username;
    private final long entityId;
    private final String serverId;
    private final Long clientId;

    /**
     * The constructor
     *
     * @param entityId the entity ID
     * @param serverId the server ID
     */
    public EntityServerCompositeKey(long entityId, String serverId)
    {
        this.username = null;
        this.entityId = entityId;
        this.serverId = serverId;
        this.clientId = null;
    }

    public EntityServerCompositeKey(String username, long entityId, String serverId, Long clientId)
    {
        this.username = username;
        this.entityId = entityId;
        this.serverId = serverId;
        this.clientId = clientId;
    }

    public Long getClientId()
    {
        return clientId;
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

        EntityServerCompositeKey that = (EntityServerCompositeKey) o;

        if (entityId != that.entityId)
        {
            return false;
        }
        if (serverId != null ? !serverId.equals(that.serverId) : that.serverId != null)
        {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null)
        {
            return false;
        }
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = (int) (entityId ^ (entityId >>> 32));
        result = 31 * result + (serverId != null ? serverId.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        return result;
    }
}
