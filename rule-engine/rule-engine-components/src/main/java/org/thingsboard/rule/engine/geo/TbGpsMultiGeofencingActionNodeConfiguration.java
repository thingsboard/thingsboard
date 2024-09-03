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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

import java.util.List;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbGpsMultiGeofencingActionNodeConfiguration extends TbGpsGeofencingFilterNodeConfiguration {

    private EntityRelationsQuery entityRelationsQuery;
    private EntitySearchDirection relationField;
    private String perimeterAttributeKey;
    private Long insideDurationMs;
    private Long outsideDurationMs;

    @Override
    public TbGpsMultiGeofencingActionNodeConfiguration defaultConfiguration() {
        TbGpsMultiGeofencingActionNodeConfiguration configuration = new TbGpsMultiGeofencingActionNodeConfiguration();
        configuration.setPerimeterAttributeKey("geofences");
        RelationsSearchParameters relationsSearchParameters = new RelationsSearchParameters(new DeviceId(UUID.randomUUID()), EntitySearchDirection.FROM, 1, true);
        EntityRelationsQuery entityRelationsQuery = new EntityRelationsQuery();
        entityRelationsQuery.setParameters(relationsSearchParameters);
        RelationEntityTypeFilter filter = new RelationEntityTypeFilter("DeviceToZone", List.of(EntityType.ASSET));
        entityRelationsQuery.setFilters(List.of(filter));
        configuration.setEntityRelationsQuery(entityRelationsQuery);
        configuration.setRelationField(EntitySearchDirection.TO);
        configuration.setInsideDurationMs(1000L);
        configuration.setOutsideDurationMs(1000L);
        return configuration;
    }
}
