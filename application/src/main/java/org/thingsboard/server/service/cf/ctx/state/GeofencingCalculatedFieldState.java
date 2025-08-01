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
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.utils.CalculatedFieldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class GeofencingCalculatedFieldState implements CalculatedFieldState {

    public static final String ENTITY_ID_LATITUDE_ARGUMENT_KEY = "latitude";
    public static final String ENTITY_ID_LONGITUDE_ARGUMENT_KEY = "longitude";
    public static final String SAVE_ZONES_ARGUMENT_KEY = "saveZones";
    public static final String RESTRICTED_ZONES_ARGUMENT_KEY = "restrictedZones";

    private List<String> requiredArguments;
    private Map<String, ArgumentEntry> arguments;

    protected boolean sizeExceedsLimit;

    private long latestTimestamp = -1;


    public GeofencingCalculatedFieldState() {
        this(List.of(ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY, SAVE_ZONES_ARGUMENT_KEY, RESTRICTED_ZONES_ARGUMENT_KEY));
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

        for (Map.Entry<String, ArgumentEntry> entry : argumentValues.entrySet()) {
            String key = entry.getKey();
            ArgumentEntry newEntry = entry.getValue();

             checkArgumentSize(key, newEntry, ctx);

            ArgumentEntry existingEntry = arguments.get(key);
            boolean entryUpdated;

            if (existingEntry == null || newEntry.isForceResetPrevious()) {
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
                entryUpdated = existingEntry.updateEntry(newEntry);
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
        List<CalculatedFieldResult> savedZonesStatesResults = updateGeofencingZonesState(ctx, false);
        List<CalculatedFieldResult> restrictedZonesStatesResults = updateGeofencingZonesState(ctx, true);

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

    @Override
    public void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize) {
        if (!sizeExceedsLimit && maxStateSize > 0 && CalculatedFieldUtils.toProto(ctxId, this).getSerializedSize() > maxStateSize) {
            arguments.clear();
            sizeExceedsLimit = true;
        }
    }

    private void updateLastUpdateTimestamp(ArgumentEntry entry) {
        long newTs = this.latestTimestamp;
        if (entry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
            newTs = singleValueArgumentEntry.getTs();
        }
        this.latestTimestamp = Math.max(this.latestTimestamp, newTs);
    }

    // TODO: Ensure all cases are covered based on rule node logic.
    private List<CalculatedFieldResult> updateGeofencingZonesState(CalculatedFieldCtx ctx, boolean restricted) {
        var results = new ArrayList<CalculatedFieldResult>();
        long stateSwitchTime = System.currentTimeMillis();
        double latitude = (double) arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY).getValue();
        double longitude = (double) arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY).getValue();

        Coordinates entityCoordinates = new Coordinates(latitude, longitude);
        String zoneKey = restricted ? RESTRICTED_ZONES_ARGUMENT_KEY : SAVE_ZONES_ARGUMENT_KEY;
        GeofencingArgumentEntry zonesEntry = (GeofencingArgumentEntry) arguments.get(zoneKey);

        for (var zoneEntry : zonesEntry.getZoneStates().entrySet()) {
            GeofencingZoneState state = zoneEntry.getValue();
            String event = state.evaluate(entityCoordinates, stateSwitchTime);
            ObjectNode stateNode = JacksonUtil.newObjectNode();
            stateNode.put("entityId", ctx.getEntityId().toString());
            stateNode.put("zoneId", state.getZoneId().toString());
            stateNode.put("restricted", restricted);
            stateNode.put("event", event);
            results.add(new CalculatedFieldResult(ctx.getOutput().getType(), ctx.getOutput().getScope(), stateNode));
        }
        return results;
    }

}
