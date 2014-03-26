package com.atlassian.confluence.extra.jira.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
        if (!(obj instanceof EntityServerCompositeKey))
        {
            return false;
        }
        EntityServerCompositeKey serverEntityCompositeKey = (EntityServerCompositeKey) obj;
        return new EqualsBuilder()
                .append(this.entityId, serverEntityCompositeKey.entityId)
                .append(this.serverId, serverEntityCompositeKey.serverId)
                .isEquals();
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
