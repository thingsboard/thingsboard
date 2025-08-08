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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.cf.configuration.CFArgumentDynamicSourceType.RELATION_QUERY;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeofencingCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    public static final String ENTITY_ID_LATITUDE_ARGUMENT_KEY = "latitude";
    public static final String ENTITY_ID_LONGITUDE_ARGUMENT_KEY = "longitude";

    public static final Set<String> coordinateKeys = Set.of(
            ENTITY_ID_LATITUDE_ARGUMENT_KEY,
            ENTITY_ID_LONGITUDE_ARGUMENT_KEY
    );

    private boolean trackRelationToZones;
    private String zoneRelationType;
    private EntitySearchDirection zoneRelationDirection;
    private Map<String, GeofencingZoneGroupConfiguration> geofencingZoneGroupConfigurations;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    // TODO: update validate method in PE version.
    //  Add relation tracking configuration validation
    @Override
    public void validate() {
        if (arguments == null) {
            throw new IllegalArgumentException("Geofencing calculated field arguments are empty!");
        }
        if (arguments.size() < 3) {
            throw new IllegalArgumentException("Geofencing calculated field must contain at least 3 arguments!");
        }
        if (arguments.size() > 5) {
            throw new IllegalArgumentException("Geofencing calculated field size exceeds limit of 5 arguments!");
        }
        validateCoordinateArguments();

        Map<String, Argument> zoneGroupsArguments = getZoneGroupArguments();
        if (zoneGroupsArguments.isEmpty()) {
            throw new IllegalArgumentException("Geofencing calculated field must contain at least one geofencing zone group defined!");
        }
        validateZoneGroupAruguments(zoneGroupsArguments);
        validateZoneGroupConfigurations(zoneGroupsArguments);
    }

    private void validateZoneGroupConfigurations(Map<String, Argument> zoneGroupsArguments) {
        if (geofencingZoneGroupConfigurations == null) {
            throw new IllegalArgumentException("Geofencing calculated field zone group configurations are empty!");
        }
        Set<String> usedPrefixes = new HashSet<>();
        geofencingZoneGroupConfigurations.forEach((zoneGroupName, config) -> {
            Argument zoneGroupArgument = zoneGroupsArguments.get(zoneGroupName);
            if (zoneGroupArgument == null) {
                throw new IllegalArgumentException("Geofencing calculated field zone group configuration is not configured for zone group: " + zoneGroupName);
            }
            if (config == null) {
                throw new IllegalArgumentException("Zone group configuration is not configured for zone group: " + zoneGroupName);
            }
            if (CollectionsUtil.isEmpty(config.getReportEvents())) {
                throw new IllegalArgumentException("Zone group configuration report events must be specified for zone group: " + zoneGroupName);
            }
            String prefix = config.getReportTelemetryPrefix();
            if (StringUtils.isBlank(prefix)) {
                throw new IllegalArgumentException("Report telemetry prefix should be specified for zone group: " + zoneGroupName);
            }
            if (!usedPrefixes.add(prefix)) {
                throw new IllegalArgumentException("Duplicate report telemetry prefix found: '" + prefix + "'. Must be unique!");
            }
        });
    }

    private void validateCoordinateArguments() {
        for (String coordinateKey : coordinateKeys) {
            Argument argument = arguments.get(coordinateKey);
            if (argument == null) {
                throw new IllegalArgumentException("Missing required coordinates argument: " + coordinateKey);
            }
            ReferencedEntityKey refEntityKey = validateAndGetRefEntityKey(argument, coordinateKey);
            if (!ArgumentType.TS_LATEST.equals(refEntityKey.getType())) {
                throw new IllegalArgumentException("Argument '" + coordinateKey + "' must be of type TS_LATEST.");
            }
            if (argument.getRefDynamicSource() != null) {
                throw new IllegalArgumentException("Dynamic source is not allowed for argument: '" + coordinateKey + "'.");
            }
        }
    }

    private void validateZoneGroupAruguments(Map<String, Argument> zoneGroupsArguments) {
        zoneGroupsArguments.forEach((argumentKey, argument) -> {
            if (argument == null) {
                throw new IllegalArgumentException("Zone group argument is not configured: " + argumentKey);
            }
            ReferencedEntityKey refEntityKey = validateAndGetRefEntityKey(argument, argumentKey);
            if (!ArgumentType.ATTRIBUTE.equals(refEntityKey.getType())) {
                throw new IllegalArgumentException("Argument '" + argumentKey + "' must be of type ATTRIBUTE.");
            }
            var dynamicSource = argument.getRefDynamicSource();
            if (dynamicSource == null) {
                return;
            }
            if (!RELATION_QUERY.equals(dynamicSource)) {
                throw new IllegalArgumentException("Only relation query dynamic source is supported for argument: '" + argumentKey + "'.");
            }
            if (argument.getRefDynamicSourceConfiguration() == null) {
                throw new IllegalArgumentException("Missing dynamic source configuration for argument: '" + argumentKey + "'.");
            }
            argument.getRefDynamicSourceConfiguration().validate();
        });
    }

    private Map<String, Argument> getZoneGroupArguments() {
        return arguments.entrySet()
                .stream()
                .filter(entry -> !coordinateKeys.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static ReferencedEntityKey validateAndGetRefEntityKey(Argument argument, String argumentKey) {
        ReferencedEntityKey refEntityKey = argument.getRefEntityKey();
        if (refEntityKey == null || refEntityKey.getType() == null) {
            throw new IllegalArgumentException("Missing or invalid reference entity key for argument: " + argumentKey);
        }
        return refEntityKey;
    }

}
