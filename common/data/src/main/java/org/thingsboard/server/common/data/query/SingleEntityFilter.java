package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

@Data
public class SingleEntityFilter implements EntityFilter {
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.SINGLE_ENTITY;
    }

    private EntityId singleEntity;

}
