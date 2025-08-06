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
import org.thingsboard.server.common.data.cf.CalculatedFieldType;

import java.util.Map;
import java.util.Set;

import static org.thingsboard.server.common.data.cf.configuration.CFArgumentDynamicSourceType.RELATION_QUERY;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeofencingCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    public static final String ENTITY_ID_LATITUDE_ARGUMENT_KEY = "latitude";
    public static final String ENTITY_ID_LONGITUDE_ARGUMENT_KEY = "longitude";
    public static final String ALLOWED_ZONES_ARGUMENT_KEY = "allowedZones";
    public static final String RESTRICTED_ZONES_ARGUMENT_KEY = "restrictedZones";

    private static final Set<String> allowedKeys = Set.of(
            ENTITY_ID_LATITUDE_ARGUMENT_KEY,
            ENTITY_ID_LONGITUDE_ARGUMENT_KEY,
            ALLOWED_ZONES_ARGUMENT_KEY,
            RESTRICTED_ZONES_ARGUMENT_KEY
    );

    private static final Set<String> requiredKeys = Set.of(
            ENTITY_ID_LATITUDE_ARGUMENT_KEY,
            ENTITY_ID_LONGITUDE_ARGUMENT_KEY
    );

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    // TODO: update validate method in PE version.
    @Override
    public void validate() {
        if (arguments == null) {
            throw new IllegalArgumentException("Geofencing calculated field arguments are empty!");
        }

        // Check key count
        if (arguments.size() < 3 || arguments.size() > 4) {
            throw new IllegalArgumentException("Geofencing calculated field must contain 3 or 4 arguments: " + allowedKeys);
        }

        // Check for unsupported argument keys
        for (String key : arguments.keySet()) {
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("Unsupported argument key: '" + key + "'. Allowed keys: " + allowedKeys);
            }
        }

        // Check required fields: latitude and longitude
        for (String requiredKey : requiredKeys) {
            if (!arguments.containsKey(requiredKey)) {
                throw new IllegalArgumentException("Missing required argument: " + requiredKey);
            }
        }

        // Ensure at least one of the zone types is configured
        boolean hasAllowedZones = arguments.containsKey(ALLOWED_ZONES_ARGUMENT_KEY);
        boolean hasRestrictedZones = arguments.containsKey(RESTRICTED_ZONES_ARGUMENT_KEY);

        if (!hasAllowedZones && !hasRestrictedZones) {
            throw new IllegalArgumentException("Geofencing calculated field must contain at least one of the following arguments: 'allowedZones' or 'restrictedZones'");
        }

        for (Map.Entry<String, Argument> entry : arguments.entrySet()) {
            String argumentKey = entry.getKey();
            Argument argument = entry.getValue();
            if (argument == null) {
                throw new IllegalArgumentException("Missing required argument: " + argumentKey);
            }
            ReferencedEntityKey refEntityKey = argument.getRefEntityKey();
            if (refEntityKey == null || refEntityKey.getType() == null) {
                throw new IllegalArgumentException("Missing or invalid reference entity key for argument: " + argumentKey);
            }

            switch (argumentKey) {
                case ENTITY_ID_LATITUDE_ARGUMENT_KEY,
                     ENTITY_ID_LONGITUDE_ARGUMENT_KEY -> {
                    if (!ArgumentType.TS_LATEST.equals(refEntityKey.getType())) {
                        throw new IllegalArgumentException("Argument '" + argumentKey + "' must be of type TS_LATEST.");
                    }
                    if (argument.getRefDynamicSource() != null) {
                        throw new IllegalArgumentException("Dynamic source is not allowed for argument: '" + argumentKey + "'.");
                    }
                }
                case ALLOWED_ZONES_ARGUMENT_KEY,
                     RESTRICTED_ZONES_ARGUMENT_KEY -> {
                    if (!ArgumentType.ATTRIBUTE.equals(refEntityKey.getType())) {
                        throw new IllegalArgumentException("Argument '" + argumentKey + "' must be of type ATTRIBUTE.");
                    }
                    var dynamicSource = argument.getRefDynamicSource();
                    if (dynamicSource == null) {
                        continue;
                    }
                    if (!RELATION_QUERY.equals(dynamicSource)) {
                        throw new IllegalArgumentException("Only relation query dynamic source is supported for argument: '" + argumentKey + "'.");
                    }
                    if (argument.getRefDynamicSourceConfiguration() == null) {
                        throw new IllegalArgumentException("Missing dynamic source configuration for argument: '" + argumentKey + "'.");
                    }
                    argument.getRefDynamicSourceConfiguration().validate();
                }
            }
        }
    }

}
