/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    public AlarmDataQuery(EntityFilter entityFilter) {
        super(entityFilter);
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
