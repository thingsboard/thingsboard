// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration.geofencing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentsBasedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.HasUseLatestTsConfig;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.ScheduledUpdateSupportedCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Schema
@Data
public class GeofencingCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration, ScheduledUpdateSupportedCalculatedFieldConfiguration, HasUseLatestTsConfig {

    @Valid
    @NotNull
    private EntityCoordinates entityCoordinates;

    @Valid
    @NotNull
    private Map<String, ZoneGroupConfiguration> zoneGroups;

    private boolean scheduledUpdateEnabled;
    private Integer scheduledUpdateInterval;

    @NotNull
    private Output output;

    @Override
    @JsonIgnore
    public boolean isUseLatestTs() {
        return output.getType() == OutputType.TIME_SERIES;
    }

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
    public Set<EntityId> getReferencedEntities() {
        return zoneGroups == null ? Collections.emptySet() : zoneGroups.values().stream()
                .map(ZoneGroupConfiguration::getRefEntityId)
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    @Override
    public Output getOutput() {
        return output;
    }

    @Override
    public void validate() {
        if (scheduledUpdateEnabled && scheduledUpdateInterval == null) {
            throw new IllegalArgumentException("Refresh interval is required when periodic zone group refresh is enabled.");
        }
        zoneGroups.forEach((key, value) -> value.validate(key));
    }

}
