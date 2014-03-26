package com.atlassian.confluence.extra.jira.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Composite key for server Id and Entity Id
 */
public class EntityServerCompositeKey
{
    private long entityId;
    private String serverId;

    /**
     * The constructor
     * @param entityId the entity ID
     * @param serverId the server ID
     */
    public EntityServerCompositeKey(long entityId, String serverId)
    {
        this.entityId = entityId;
        this.serverId = serverId;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }
        if (obj.getClass() != getClass())
        {
            return false;
        }
        EntityServerCompositeKey serverEntityCompositeKey = (EntityServerCompositeKey) obj;
        return entityId == serverEntityCompositeKey.entityId && serverId.equals(serverEntityCompositeKey.serverId);
    }

    @Override
    public int hashCode()
    {
        // you pick a hard-coded, randomly chosen, non-zero, odd number
        // ideally different for each class
        return new HashCodeBuilder(17, 37)
                .append(entityId)
                .append(serverId)
                .toHashCode();
    }
}
