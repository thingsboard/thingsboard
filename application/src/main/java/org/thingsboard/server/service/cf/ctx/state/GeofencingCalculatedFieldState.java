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
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.common.util.geo.PerimeterDefinition;
import org.thingsboard.rule.engine.geo.EntityGeofencingState;
import org.thingsboard.rule.engine.util.GpsGeofencingEvents;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class GeofencingCalculatedFieldState implements CalculatedFieldState {

    public static final String ENTITY_ID_LATITUDE_ARGUMENT_KEY = "latitude";
    public static final String ENTITY_ID_LONGITUDE_ARGUMENT_KEY = "longitude";
    public static final String SAVE_ZONES_ARGUMENT_KEY = "saveZones";
    public static final String RESTRICTED_ZONES_ARGUMENT_KEY = "restrictedZones";

    private List<String> requiredArguments;
    private Map<String, ArgumentEntry> arguments;
    private long latestTimestamp = -1;

    private Map<EntityId, EntityGeofencingState> saveZoneStates;
    private Map<EntityId, EntityGeofencingState> restrictedZoneStates;

    public GeofencingCalculatedFieldState() {
        this(List.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY, SAVE_ZONES_ARGUMENT_KEY, RESTRICTED_ZONES_ARGUMENT_KEY));
    }

    public GeofencingCalculatedFieldState(List<String> argNames) {
        this.requiredArguments = argNames;
        this.arguments = new HashMap<>();
        this.saveZoneStates = new HashMap<>();
        this.restrictedZoneStates = new HashMap<>();
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    @Override
    public boolean updateState(CalculatedFieldCtx ctx, Map<String, ArgumentEntry> argumentValues) {
        // TODO: Do I need to check argument for null?
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        boolean stateUpdated = false;

        for (Map.Entry<String, ArgumentEntry> entry : argumentValues.entrySet()) {
            String key = entry.getKey();
            ArgumentEntry newEntry = entry.getValue();

            // TODO: Do I need to check argument size?
            // checkArgumentSize(key, newEntry, ctx);

            ArgumentEntry existingEntry = arguments.get(key);
            boolean entryUpdated;

            // TODO: What is force reset previos?
            //  if (existingEntry == null || newEntry.isForceResetPrevious()) {

            // fresh start of state. No entry exists yet.
            if (existingEntry == null) {
                switch (key) {
                    case ENTITY_ID_LATITUDE_ARGUMENT_KEY:
                    case ENTITY_ID_LONGITUDE_ARGUMENT_KEY:
                        if (!(newEntry instanceof SingleValueArgumentEntry singleValueArgumentEntry)) {
                            throw new IllegalArgumentException(key + " argument must be a single value argument.");
                        }
                        arguments.put(key, singleValueArgumentEntry);
                        entryUpdated = true;
                        break;
                    case SAVE_ZONES_ARGUMENT_KEY:
                    case RESTRICTED_ZONES_ARGUMENT_KEY:
                        if (!(newEntry instanceof GeofencingArgumentEntry geofencingArgumentEntry)) {
                            throw new IllegalArgumentException(key + " argument must be a geofencing argument entry.");
                        }
                        arguments.put(key, geofencingArgumentEntry);
                        entryUpdated = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported argument: " + key);
                }
            } else {
                entryUpdated = switch (key) {
                    case ENTITY_ID_LATITUDE_ARGUMENT_KEY,
                         ENTITY_ID_LONGITUDE_ARGUMENT_KEY -> existingEntry.updateEntry(newEntry);
                    case SAVE_ZONES_ARGUMENT_KEY,
                         RESTRICTED_ZONES_ARGUMENT_KEY -> {
                            // TODO: ensure zone cleanup working correctly.
                            boolean updated = existingEntry.updateEntry(newEntry);
                            if (updated) {
                                Map<EntityId, EntityGeofencingState> currentStates =
                                        key.equals(SAVE_ZONES_ARGUMENT_KEY) ? saveZoneStates : restrictedZoneStates;
                                Set<EntityId> newZoneIds = ((GeofencingArgumentEntry) newEntry).getGeofencingIdToPerimeter().keySet();
                                currentStates.keySet().removeIf(existingZoneId -> !newZoneIds.contains(existingZoneId));
                            }
                            yield updated;
                         }
                    default -> throw new IllegalStateException("Unsupported argument: " + key);
                };
            }

            if (entryUpdated) {
                stateUpdated = true;
                updateLastUpdateTimestamp(newEntry);
            }
        }
        return stateUpdated;
    }


    @Override
    public ListenableFuture<List<CalculatedFieldResult>> performCalculation(CalculatedFieldCtx ctx) {
        List<CalculatedFieldResult> savedZonesStatesResults = updateSavedGeofencingZonesState(ctx);
        List<CalculatedFieldResult> restrictedZonesStatesResults = updateRestrictedGeofencingZonesState(ctx);

        List<CalculatedFieldResult> allZoneStatesResults =
                new ArrayList<>(savedZonesStatesResults.size() + restrictedZonesStatesResults.size());
        allZoneStatesResults.addAll(savedZonesStatesResults);
        allZoneStatesResults.addAll(restrictedZonesStatesResults);

        return Futures.immediateFuture(allZoneStatesResults);
    }

    @Override
    public boolean isReady() {
        return arguments.keySet().containsAll(requiredArguments) &&
               arguments.values().stream().noneMatch(ArgumentEntry::isEmpty);
    }

    // TODO: implement
    @Override
    public boolean isSizeExceedsLimit() {
        return false;
    }

    // TODO: implement
    @Override
    public void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize) {

    }

    // TODO: implement
    @Override
    public void checkArgumentSize(String name, ArgumentEntry entry, CalculatedFieldCtx ctx) {

    }

    private void updateLastUpdateTimestamp(ArgumentEntry entry) {
        long newTs = this.latestTimestamp;
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            newTs = singleValueArgumentEntry.getTs();
        }
        this.latestTimestamp = Math.max(this.latestTimestamp, newTs);
    }

    private List<CalculatedFieldResult> updateSavedGeofencingZonesState(CalculatedFieldCtx ctx) {
        return updateGeofencingZonesState(ctx, saveZoneStates, false);
    }

    private List<CalculatedFieldResult> updateRestrictedGeofencingZonesState(CalculatedFieldCtx ctx) {
        return updateGeofencingZonesState(ctx, restrictedZoneStates, true);
    }

    // TODO: Ensure all cases are covered based on rule node logic.
    private List<CalculatedFieldResult> updateGeofencingZonesState(CalculatedFieldCtx ctx, Map<EntityId, EntityGeofencingState> zoneStates, boolean restricted) {
        var results = new ArrayList<CalculatedFieldResult>();

        long stateSwitchTime = System.currentTimeMillis();
        double latitude = (double) arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY).getValue();
        double longitude = (double) arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY).getValue();
        Coordinates entityCoordinates = new Coordinates(latitude, longitude);

        String zoneKey = restricted ? RESTRICTED_ZONES_ARGUMENT_KEY : SAVE_ZONES_ARGUMENT_KEY;
        GeofencingArgumentEntry zonesEntry = (GeofencingArgumentEntry) arguments.get(zoneKey);

        for (Map.Entry<EntityId, PerimeterDefinition> entry : zonesEntry.getGeofencingIdToPerimeter().entrySet()) {
            EntityId zoneId = entry.getKey();
            PerimeterDefinition perimeter = entry.getValue();

            boolean inside = perimeter.checkMatches(entityCoordinates);

            // Always present or created
            EntityGeofencingState state = zoneStates.computeIfAbsent(
                    zoneId, id -> new EntityGeofencingState(false, 0L, false)
            );

            String event;
            if (state.getStateSwitchTime() == 0L || state.isInside() != inside) {
                // First state or transition (entered/left)
                state.setInside(inside);
                state.setStateSwitchTime(stateSwitchTime);
                state.setStayed(false);

                event = inside ? GpsGeofencingEvents.ENTERED : GpsGeofencingEvents.LEFT;
            } else {
                // No transition
                event = inside ? GpsGeofencingEvents.INSIDE : GpsGeofencingEvents.OUTSIDE;
            }

            ObjectNode stateNode = JacksonUtil.newObjectNode();
            stateNode.put("entityId", ctx.getEntityId().toString());
            stateNode.put("zoneId", zoneId.getId().toString());
            stateNode.put("restricted", restricted);
            stateNode.put("event", event);

            results.add(new CalculatedFieldResult(ctx.getOutput().getType(), ctx.getOutput().getScope(), stateNode));
        }

        return results;
    }

}
