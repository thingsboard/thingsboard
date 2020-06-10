package org.thingsboard.server.common.data.query;

import lombok.Getter;

import java.util.List;

public class EntityDataQuery extends EntityCountQuery {

    @Getter
    private final EntityDataPageLink pageLink;
    @Getter
    private final List<EntityKey> entityFields;
    @Getter
    private final List<EntityKey> latestValues;

    public EntityDataQuery(EntityFilter entityFilter, EntityDataPageLink pageLink, List<EntityKey> entityFields, List<EntityKey> latestValues) {
        super(entityFilter);
        this.pageLink = pageLink;
        this.entityFields = entityFields;
        this.latestValues = latestValues;
    }
}
