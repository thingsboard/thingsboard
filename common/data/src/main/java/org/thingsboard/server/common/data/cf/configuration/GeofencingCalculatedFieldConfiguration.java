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

@Data
@EqualsAndHashCode(callSuper = true)
public class GeofencingCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    public static final String ENTITY_ID_LATITUDE_ARGUMENT_KEY = "latitude";
    public static final String ENTITY_ID_LONGITUDE_ARGUMENT_KEY = "longitude";

    private static final Set<String> coordinateKeys = Set.of(
            ENTITY_ID_LATITUDE_ARGUMENT_KEY,
            ENTITY_ID_LONGITUDE_ARGUMENT_KEY
    );

    private int scheduledUpdateIntervalSec;

    private boolean createRelationsWithMatchedZones;
    private String zoneRelationType;
    private EntitySearchDirection zoneRelationDirection;
    private Map<String, ZoneGroupConfiguration> zoneGroupConfigurations;

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    @Override
    public boolean isScheduledUpdateEnabled() {
        return scheduledUpdateIntervalSec > 0 && arguments.values().stream().anyMatch(Argument::hasDynamicSource);
    }

    // TODO: update validate method in PE version.
    @Override
    public void validate() {
        if (arguments == null) {
            throw new IllegalArgumentException("Geofencing calculated field arguments must be specified!");
        }
        validateCoordinateArguments();
        Map<String, Argument> zoneGroupsArguments = getZoneGroupArguments();
        if (zoneGroupsArguments.isEmpty()) {
            throw new IllegalArgumentException("Geofencing calculated field must contain at least one geofencing zone group defined!");
        }
        validateZoneGroupAruguments(zoneGroupsArguments);
        validateZoneGroupConfigurations(zoneGroupsArguments);
        validateZoneRelationsConfiguration();
    }

    private void validateZoneRelationsConfiguration() {
        if (!createRelationsWithMatchedZones) {
            return;
        }
        if (StringUtils.isBlank(zoneRelationType)) {
            throw new IllegalArgumentException("Zone relation type must be specified to create relations with matched zones!");
        }
        if (zoneRelationDirection == null) {
            throw new IllegalArgumentException("Zone relation direction must be specified to create relations with matched zones!");
        }
    }

    private void validateZoneGroupConfigurations(Map<String, Argument> zoneGroupsArguments) {
        if (zoneGroupConfigurations == null || zoneGroupConfigurations.isEmpty()) {
            throw new IllegalArgumentException("Zone groups configuration should be specified!");
        }
        Set<String> usedPrefixes = new HashSet<>();

        zoneGroupsArguments.forEach((zoneGroupName, zoneGroupArgument) -> {
            ZoneGroupConfiguration config = zoneGroupConfigurations.get(zoneGroupName);
            if (config == null) {
                throw new IllegalArgumentException("Zone group configuration is not configured for '" + zoneGroupName + "' argument!");
            }
            if (CollectionsUtil.isEmpty(config.getReportEvents())) {
                throw new IllegalArgumentException("Zone group configuration report events must be specified for '" + zoneGroupName + "' argument!");
            }
            String prefix = config.getReportTelemetryPrefix();
            if (StringUtils.isBlank(prefix)) {
                throw new IllegalArgumentException("Report telemetry prefix should be specified for '" + zoneGroupName + "' argument!");
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
                throw new IllegalArgumentException("Missing required coordinates argument: " + coordinateKey + "!");
            }
            ReferencedEntityKey refEntityKey = validateAndGetRefEntityKey(argument, coordinateKey);
            if (!ArgumentType.TS_LATEST.equals(refEntityKey.getType())) {
                throw new IllegalArgumentException("Argument '" + coordinateKey + "' must be of type TS_LATEST!");
            }
            if (argument.hasDynamicSource()) {
                throw new IllegalArgumentException("Dynamic source is not allowed for '" + coordinateKey + "' argument!");
            }
        }
    }

    private void validateZoneGroupAruguments(Map<String, Argument> zoneGroupsArguments) {
        zoneGroupsArguments.forEach((argumentKey, argument) -> {
            ReferencedEntityKey refEntityKey = validateAndGetRefEntityKey(argument, argumentKey);
            if (!ArgumentType.ATTRIBUTE.equals(refEntityKey.getType())) {
                throw new IllegalArgumentException("Argument '" + argumentKey + "' must be of type ATTRIBUTE!");
            }
            if (argument.hasDynamicSource()) {
                argument.getRefDynamicSourceConfiguration().validate();
            }
        });
    }

    private Map<String, Argument> getZoneGroupArguments() {
        return arguments.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !coordinateKeys.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ReferencedEntityKey validateAndGetRefEntityKey(Argument argument, String argumentKey) {
        ReferencedEntityKey refEntityKey = argument.getRefEntityKey();
        if (refEntityKey == null || refEntityKey.getType() == null) {
            throw new IllegalArgumentException("Missing or invalid reference entity key for argument: " + argumentKey);
        }
        return refEntityKey;
    }

}
