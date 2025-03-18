/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbGpsMultiGeofencingActionNodeConfiguration extends TbGpsGeofencingActionNodeConfiguration {

    private RelationsQuery relationsQuery;
    private String metadataDurationConfigKey;

    @Override
    public TbGpsMultiGeofencingActionNodeConfiguration defaultConfiguration() {
        TbGpsMultiGeofencingActionNodeConfiguration configuration = new TbGpsMultiGeofencingActionNodeConfiguration();
        configuration.setPerimeterKeyName("geofences");
        configuration.setLatitudeKeyName("latitude");
        configuration.setLongitudeKeyName("longitude");
        RelationsQuery relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter filter = new RelationEntityTypeFilter("DeviceToZone", List.of(EntityType.ASSET));
        relationsQuery.setFilters(List.of(filter));
        configuration.setRelationsQuery(relationsQuery);
        configuration.setMinInsideDuration(1);
        configuration.setMinInsideDurationTimeUnit("MINUTES");
        configuration.setMinOutsideDuration(1);
        configuration.setMinOutsideDurationTimeUnit("MINUTES");
        configuration.setMetadataDurationConfigKey("durationConfig");
        return configuration;
    }
}
