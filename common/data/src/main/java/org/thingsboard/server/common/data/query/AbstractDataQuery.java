// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString(callSuper = true)
public abstract class AbstractDataQuery<T extends EntityDataPageLink> extends EntityCountQuery {

    @Getter
    protected T pageLink;
    @Getter
    protected List<EntityKey> entityFields;
    @Getter
    protected List<EntityKey> latestValues;

    public AbstractDataQuery() {
        super();
    }

    public AbstractDataQuery(EntityFilter entityFilter, List<KeyFilter> keyFilters) {
        super(entityFilter, keyFilters);
    }

    public AbstractDataQuery(EntityFilter entityFilter,
                             T pageLink,
                             List<EntityKey> entityFields,
                             List<EntityKey> latestValues,
                             List<KeyFilter> keyFilters) {
        super(entityFilter, keyFilters);
        this.pageLink = pageLink;
        this.entityFields = entityFields;
        this.latestValues = latestValues;
    }

}
