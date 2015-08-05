package com.atlassian.confluence.extra.jira.model;

/**
 * Composite key for server Id and Entity Id
 */
public class EntityServerCompositeKey
{
    private final long entityId;
    private final String serverId;

    /**
     * The constructor
     *
     * @param entityId the entity ID
     * @param serverId the server ID
     */
    public EntityServerCompositeKey(long entityId, String serverId)
    {
        this.entityId = entityId;
        this.serverId = serverId;
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
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = (int) (entityId ^ (entityId >>> 32));
        result = 31 * result + (serverId != null ? serverId.hashCode() : 0);
        return result;
    }
}
