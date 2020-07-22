/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
public class EntityDataQuery extends AbstractDataQuery<EntityDataPageLink> {

    public EntityDataQuery() {
    }

    public EntityDataQuery(EntityFilter entityFilter) {
        super(entityFilter);
    }

    public EntityDataQuery(EntityFilter entityFilter, EntityDataPageLink pageLink, List<EntityKey> entityFields, List<EntityKey> latestValues, List<KeyFilter> keyFilters) {
        super(entityFilter, pageLink, entityFields, latestValues, keyFilters);
    }

    @JsonIgnore
    public EntityDataQuery next() {
        return new EntityDataQuery(getEntityFilter(), getPageLink().nextPageLink(), entityFields, latestValues, keyFilters);
    }

}
