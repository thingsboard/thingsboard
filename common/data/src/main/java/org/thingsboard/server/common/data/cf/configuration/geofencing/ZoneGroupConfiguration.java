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
package org.thingsboard.server.common.data.cf.configuration.geofencing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.lang.Nullable;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CfArgumentDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZoneGroupConfiguration {

    @Nullable
    private EntityId refEntityId;
    private CfArgumentDynamicSourceConfiguration refDynamicSourceConfiguration;

    @NotBlank
    private final String perimeterKeyName;

    @NotNull
    private final GeofencingReportStrategy reportStrategy;
    private final boolean createRelationsWithMatchedZones;

    private String relationType;
    private EntitySearchDirection direction;

    public void validate(String name) {
        if (EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY.equals(name) || EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY.equals(name)) {
            throw new IllegalArgumentException("Name '" + name + "' is reserved and cannot be used for zone group!");
        }
        if (refDynamicSourceConfiguration != null) {
            refDynamicSourceConfiguration.validate();
        }
        if (!createRelationsWithMatchedZones) {
            return;
        }
        if (StringUtils.isBlank(relationType)) {
            throw new IllegalArgumentException("Relation type must be specified for '" + name + "' zone group!");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Relation direction must be specified for '" + name + "' zone group!");
        }
    }

    public boolean hasRelationQuerySource() {
        return toArgument().hasRelationQuerySource();
    }

    public boolean hasCurrentOwnerSource() {
        return toArgument().hasOwnerSource();
    }

    @JsonIgnore
    public boolean isCfEntitySource(EntityId cfEntityId) {
        if (refEntityId == null && refDynamicSourceConfiguration == null) {
            return true;
        }
        return refEntityId != null && refEntityId.equals(cfEntityId);
    }

    @JsonIgnore
    public boolean isLinkedCfEntitySource(EntityId cfEntityId) {
        return refEntityId != null && !refEntityId.equals(cfEntityId);
    }

    public Argument toArgument() {
        var argument = new Argument();
        argument.setRefEntityId(refEntityId);
        argument.setRefDynamicSourceConfiguration(refDynamicSourceConfiguration);
        argument.setRefEntityKey(new ReferencedEntityKey(perimeterKeyName, ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        return argument;
    }

}
