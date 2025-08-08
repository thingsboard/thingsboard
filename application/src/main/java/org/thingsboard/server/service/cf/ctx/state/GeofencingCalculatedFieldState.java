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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.GeofencingEvent;
import org.thingsboard.server.common.data.cf.configuration.GeofencingZoneGroupConfiguration;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.utils.CalculatedFieldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration.coordinateKeys;

@Data
@AllArgsConstructor
public class GeofencingCalculatedFieldState implements CalculatedFieldState {

    private List<String> requiredArguments;
    private Map<String, ArgumentEntry> arguments;
    private boolean sizeExceedsLimit;

    private long latestTimestamp = -1;

    private boolean dirty;

    public GeofencingCalculatedFieldState() {
        this(new ArrayList<>(), new HashMap<>(), false, -1, false);
    }

    public GeofencingCalculatedFieldState(List<String> argNames) {
        this.requiredArguments = argNames;
        this.arguments = new HashMap<>();
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    @Override
    public boolean updateState(CalculatedFieldCtx ctx, Map<String, ArgumentEntry> argumentValues) {
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        boolean stateUpdated = false;

        for (var entry : argumentValues.entrySet()) {
            String key = entry.getKey();
            ArgumentEntry newEntry = entry.getValue();

            checkArgumentSize(key, newEntry, ctx);

            ArgumentEntry existingEntry = arguments.get(key);
            boolean entryUpdated;

            if (existingEntry == null || newEntry.isForceResetPrevious()) {
                entryUpdated = switch (key) {
                    case ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY -> {
                        if (!(newEntry instanceof SingleValueArgumentEntry singleValueArgumentEntry)) {
                            throw new IllegalArgumentException(key + " argument must be a single value argument.");
                        }
                        arguments.put(key, singleValueArgumentEntry);
                        yield true;
                    }
                    default -> {
                        if (!(newEntry instanceof GeofencingArgumentEntry geofencingArgumentEntry)) {
                            throw new IllegalArgumentException(key + " argument must be a geofencing argument entry.");
                        }
                        arguments.put(key, geofencingArgumentEntry);
                        yield true;
                    }
                };
            } else {
                entryUpdated = existingEntry.updateEntry(newEntry);
            }
            if (entryUpdated) {
                stateUpdated = true;
            }
        }
        return stateUpdated;
    }


    // TODO: Probably returning list of CalculatedFieldResult no needed anymore,
    //  since logic changed to use zone groups with telemetry prefix.
    @Override
    public ListenableFuture<List<CalculatedFieldResult>> performCalculation(CalculatedFieldCtx ctx) {
        double latitude = (double) arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY).getValue();
        double longitude = (double) arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY).getValue();
        Coordinates entityCoordinates = new Coordinates(latitude, longitude);

        var configuration = (GeofencingCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        Map<String, GeofencingZoneGroupConfiguration> geofencingZoneGroupConfigurations = configuration.getGeofencingZoneGroupConfigurations();

        ObjectNode resultNode = JacksonUtil.newObjectNode();
        getGeofencingArguments().forEach((argumentKey, argumentEntry) -> {
            var zoneGroupConfig = geofencingZoneGroupConfigurations.get(argumentKey);
            Set<GeofencingEvent> zoneEvents = argumentEntry.getZoneStates()
                    .values()
                    .stream()
                    .map(zoneState -> zoneState.evaluate(entityCoordinates))
                    .collect(Collectors.toSet());
            aggregateZoneGroupEvent(zoneEvents)
                    .filter(geofencingEvent -> zoneGroupConfig.getReportEvents().contains(geofencingEvent))
                    .ifPresent(event ->
                            resultNode.put(zoneGroupConfig.getReportTelemetryPrefix() + "Event", event.name())
                    );
        });
        return Futures.immediateFuture(List.of(new CalculatedFieldResult(ctx.getOutput().getType(), ctx.getOutput().getScope(), resultNode)));
    }

    @Override
    public boolean isReady() {
        return arguments.keySet().containsAll(requiredArguments) &&
               arguments.values().stream().noneMatch(ArgumentEntry::isEmpty);
    }

    @Override
    public void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize) {
        if (!sizeExceedsLimit && maxStateSize > 0 && CalculatedFieldUtils.toProto(ctxId, this).getSerializedSize() > maxStateSize) {
            arguments.clear();
            sizeExceedsLimit = true;
        }
    }

    // TODO: Create a new class field to not do this on each calculation.
    private Map<String, GeofencingArgumentEntry> getGeofencingArguments() {
        return arguments.entrySet()
                .stream()
                .filter(entry -> !coordinateKeys.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (GeofencingArgumentEntry) entry.getValue()));
    }

    private Optional<GeofencingEvent> aggregateZoneGroupEvent(Set<GeofencingEvent> zoneEvents) {
        boolean hasEntered = false;
        boolean hasLeft = false;
        boolean hasInside = false;
        boolean hasOutside = false;

        for (GeofencingEvent event : zoneEvents) {
            if (event == null) {
                continue;
            }
            switch (event) {
                case ENTERED -> hasEntered = true;
                case LEFT -> hasLeft = true;
                case INSIDE -> hasInside = true;
                case OUTSIDE -> hasOutside = true;
            }
        }

        if (hasOutside && !hasInside && !hasEntered && !hasLeft) {
            return Optional.of(GeofencingEvent.OUTSIDE);
        }
        if (hasLeft && !hasEntered && !hasInside) {
            return Optional.of(GeofencingEvent.LEFT);
        }
        if (hasEntered && !hasLeft && !hasInside) {
            return Optional.of(GeofencingEvent.ENTERED);
        }
        if (hasInside || hasEntered) {
            return Optional.of(GeofencingEvent.INSIDE);
        }
        return Optional.empty();
    }

}
