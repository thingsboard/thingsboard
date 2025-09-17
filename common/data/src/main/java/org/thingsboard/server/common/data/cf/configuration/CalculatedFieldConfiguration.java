/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleCalculatedFieldConfiguration.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = ScriptCalculatedFieldConfiguration.class, name = "SCRIPT"),
        @JsonSubTypes.Type(value = GeofencingCalculatedFieldConfiguration.class, name = "GEOFENCING")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface CalculatedFieldConfiguration {

    @JsonIgnore
    CalculatedFieldType getType();

    Output getOutput();

    void validate();

    @JsonIgnore
    default List<EntityId> getReferencedEntities() {
        return Collections.emptyList();
    }

    default CalculatedFieldLink buildCalculatedFieldLink(TenantId tenantId, EntityId referencedEntityId, CalculatedFieldId calculatedFieldId) {
        CalculatedFieldLink link = new CalculatedFieldLink();
        link.setTenantId(tenantId);
        link.setEntityId(referencedEntityId);
        link.setCalculatedFieldId(calculatedFieldId);
        return link;
    }

    default List<CalculatedFieldLink> buildCalculatedFieldLinks(TenantId tenantId, EntityId cfEntityId, CalculatedFieldId calculatedFieldId) {
        return getReferencedEntities().stream()
                .filter(referencedEntity -> !referencedEntity.equals(cfEntityId))
                .map(referencedEntityId -> buildCalculatedFieldLink(tenantId, referencedEntityId, calculatedFieldId))
                .collect(Collectors.toList());
    }

}
