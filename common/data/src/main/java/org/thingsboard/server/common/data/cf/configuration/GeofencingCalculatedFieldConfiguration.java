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
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class GeofencingCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration, ScheduleSupportedCalculatedFieldConfiguration {

    private EntityCoordinates entityCoordinates;
    private List<ZoneGroupConfiguration> zoneGroups;
    private int scheduledUpdateIntervalSec;

    private Output output;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    @Override
    @JsonIgnore
    public Map<String, Argument> getArguments() {
        Map<String, Argument> args = new HashMap<>(entityCoordinates.toArguments());
        zoneGroups.forEach(zg -> args.put(zg.getName(), zg.toArgument()));
        return args;
    }

    @Override
    public Output getOutput() {
        return output;
    }

    @Override
    public boolean isScheduledUpdateEnabled() {
        return scheduledUpdateIntervalSec > 0 && zoneGroups.stream().anyMatch(ZoneGroupConfiguration::hasDynamicSource);
    }

    @Override
    public void validate() {
        if (entityCoordinates == null) {
            throw new IllegalArgumentException("Geofencing calculated field entity coordinates must be specified!");
        }
        if (zoneGroups == null || zoneGroups.isEmpty()) {
            throw new IllegalArgumentException("Geofencing calculated field must contain at least one geofencing zone group defined!");
        }
        entityCoordinates.validate();
        Set<String> seen = new HashSet<>();
        for (var zg : zoneGroups) {
            if (!seen.add(zg.getName())) {
                throw new IllegalArgumentException("Geofencing calculated field zone group name must be unique!");
            }
            zg.validate();
        }
    }

}
