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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
public class GeofencingCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration, ScheduledUpdateSupportedCalculatedFieldConfiguration {

    @Valid
    @NotNull
    private EntityCoordinates entityCoordinates;

    @Valid
    @NotNull
    private Map<String, ZoneGroupConfiguration> zoneGroups;

    private boolean scheduledUpdateEnabled;
    private int scheduledUpdateInterval;

    private Output output;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    @Override
    @JsonIgnore
    public Map<String, Argument> getArguments() {
        Map<String, Argument> args = new HashMap<>(entityCoordinates.toArguments());
        zoneGroups.forEach((zgName, zgConfig) -> args.put(zgName, zgConfig.toArgument()));
        return args;
    }


    @Override
    public List<EntityId> getReferencedEntities() {
        return zoneGroups == null ? List.of() : zoneGroups.values().stream().map(ZoneGroupConfiguration::getRefEntityId).filter(Objects::nonNull).toList();
    }

    @Override
    public Output getOutput() {
        return output;
    }

    @Override
    public void validate() {
        zoneGroups.forEach((key, value) -> value.validate(key));
    }

}
