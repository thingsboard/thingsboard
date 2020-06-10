package org.thingsboard.server.common.data.query;

import lombok.Getter;

public class EntityCountQuery {

    @Getter
    private final EntityFilter entityFilter;

    public EntityCountQuery(EntityFilter entityFilter) {
        this.entityFilter = entityFilter;
    }
}
