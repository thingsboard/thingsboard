// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

import java.util.List;

@ToString(callSuper = true)
public class EntityDataQuery extends AbstractDataQuery<EntityDataPageLink> {

    public EntityDataQuery() {
    }

    public EntityDataQuery(EntityFilter entityFilter, List<KeyFilter> keyFilters) {
        super(entityFilter, keyFilters);
    }

    public EntityDataQuery(EntityFilter entityFilter, EntityDataPageLink pageLink, List<EntityKey> entityFields, List<EntityKey> latestValues, List<KeyFilter> keyFilters) {
        super(entityFilter, pageLink, entityFields, latestValues, keyFilters);
    }

    @JsonIgnore
    public EntityDataQuery next() {
        return new EntityDataQuery(getEntityFilter(), getPageLink().nextPageLink(), entityFields, latestValues, keyFilters);
    }

}
