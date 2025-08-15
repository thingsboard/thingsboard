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
import com.google.common.util.concurrent.MoreExecutors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.GeofencingEvent;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.utils.CalculatedFieldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.GeofencingCalculatedFieldConfiguration.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;

@Data
@AllArgsConstructor
public class GeofencingCalculatedFieldState implements CalculatedFieldState {

    private List<String> requiredArguments;
    Map<String, ArgumentEntry> arguments;
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
                            throw new IllegalArgumentException("Unsupported argument entry type for " + key + " argument: " + newEntry.getType() + ". " +
                                                               "Only SINGLE_VALUE type is allowed.");
                        }
                        arguments.put(key, singleValueArgumentEntry);
                        yield true;
                    }
                    default -> {
                        if (!(newEntry instanceof GeofencingArgumentEntry geofencingArgumentEntry)) {
                            throw new IllegalArgumentException("Unsupported argument entry type for " + key + " argument: " + newEntry.getType() + ". " +
                                                               "Only GEOFENCING type is allowed.");
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

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(EntityId entityId, CalculatedFieldCtx ctx) {
        double latitude = (double) arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY).getValue();
        double longitude = (double) arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY).getValue();
        Coordinates entityCoordinates = new Coordinates(latitude, longitude);

        var configuration = (GeofencingCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        if (configuration.isCreateRelationsWithMatchedZones()) {
            return calculateWithRelations(entityId, ctx, entityCoordinates, configuration);
        }
        return calculateWithoutRelations(ctx, entityCoordinates, configuration);
    }

    private ListenableFuture<CalculatedFieldResult> calculateWithRelations(
            EntityId entityId,
            CalculatedFieldCtx ctx,
            Coordinates entityCoordinates,
            GeofencingCalculatedFieldConfiguration configuration) {

        var geofencingZoneGroupConfigurations = configuration.getZoneGroupConfigurations();

        Map<EntityId, GeofencingEvent> zoneEventMap = new HashMap<>();
        ObjectNode resultNode = JacksonUtil.newObjectNode();

        getGeofencingArguments().forEach((argumentKey, argumentEntry) -> {
            var zoneGroupConfig = geofencingZoneGroupConfigurations.get(argumentKey);
            Set<GeofencingEvent> groupEvents = new HashSet<>();

            argumentEntry.getZoneStates().forEach((zoneId, zoneState) -> {
                GeofencingEvent event = zoneState.evaluate(entityCoordinates);
                zoneEventMap.put(zoneId, event);
                groupEvents.add(event);
            });

            aggregateZoneGroupEvent(groupEvents)
                    .filter(zoneGroupConfig.getReportEvents()::contains)
                    .ifPresent(geofencingGroupEvent ->
                            resultNode.put(zoneGroupConfig.getReportTelemetryPrefix() + "Event", geofencingGroupEvent.name()));
        });

        var result = calculationResult(ctx, resultNode);

        List<ListenableFuture<Boolean>> relationFutures = zoneEventMap.entrySet().stream()
                .filter(entry -> entry.getValue().isTransitionEvent())
                .map(entry -> {
                    EntityRelation relation = toRelation(entry.getKey(), entityId, configuration);
                    return switch (entry.getValue()) {
                        case ENTERED -> ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), relation);
                        case LEFT -> ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), relation);
                        default -> throw new IllegalStateException("Unexpected transition event: " + entry.getValue());
                    };
                })
                .toList();

        if (relationFutures.isEmpty()) {
            return Futures.immediateFuture(result);
        }

        return Futures.whenAllComplete(relationFutures).call(() ->
                        new CalculatedFieldResult(ctx.getOutput().getType(), ctx.getOutput().getScope(), resultNode),
                MoreExecutors.directExecutor());
    }

    private ListenableFuture<CalculatedFieldResult> calculateWithoutRelations(
            CalculatedFieldCtx ctx,
            Coordinates entityCoordinates,
            GeofencingCalculatedFieldConfiguration configuration) {

        var geofencingZoneGroupConfigurations = configuration.getZoneGroupConfigurations();
        ObjectNode resultNode = JacksonUtil.newObjectNode();

        getGeofencingArguments().forEach((argumentKey, argumentEntry) -> {
            var zoneGroupConfig = geofencingZoneGroupConfigurations.get(argumentKey);
            Set<GeofencingEvent> groupEvents = argumentEntry.getZoneStates().values().stream()
                    .map(zs -> zs.evaluate(entityCoordinates))
                    .collect(Collectors.toSet());
            aggregateZoneGroupEvent(groupEvents)
                    .filter(zoneGroupConfig.getReportEvents()::contains)
                    .ifPresent(e -> resultNode.put(
                            zoneGroupConfig.getReportTelemetryPrefix() + "Event",
                            e.name()));
        });
        return Futures.immediateFuture(calculationResult(ctx, resultNode));
    }

    private CalculatedFieldResult calculationResult(CalculatedFieldCtx ctx, ObjectNode resultNode) {
        return new CalculatedFieldResult(ctx.getOutput().getType(), ctx.getOutput().getScope(), resultNode);
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

    private Map<String, GeofencingArgumentEntry> getGeofencingArguments() {
        return arguments.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getType().equals(ArgumentEntryType.GEOFENCING))
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

    private EntityRelation toRelation(EntityId zoneId, EntityId entityId, GeofencingCalculatedFieldConfiguration configuration) {
        return switch (configuration.getZoneRelationDirection()) {
            case TO -> new EntityRelation(zoneId, entityId, configuration.getZoneRelationType());
            case FROM -> new EntityRelation(entityId, zoneId, configuration.getZoneRelationType());
        };
    }

}
