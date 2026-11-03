// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
public class AlarmDataQuery extends AbstractDataQuery<AlarmDataPageLink> {

    @Getter
    protected List<EntityKey> alarmFields;

    public AlarmDataQuery() {
    }

    public AlarmDataQuery(EntityFilter entityFilter, List<KeyFilter> keyFilters) {
        super(entityFilter, keyFilters);
    }

    public AlarmDataQuery(EntityFilter entityFilter, AlarmDataPageLink pageLink, List<EntityKey> entityFields, List<EntityKey> latestValues, List<KeyFilter> keyFilters, List<EntityKey> alarmFields) {
        super(entityFilter, pageLink, entityFields, latestValues, keyFilters);
        this.alarmFields = alarmFields;
    }

    @JsonIgnore
    public AlarmDataQuery next() {
        return new AlarmDataQuery(getEntityFilter(), getPageLink().nextPageLink(), entityFields, latestValues, keyFilters, alarmFields);
    }
}
