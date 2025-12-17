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
package org.thingsboard.server.service.cf.ctx.state.geofencing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.geo.Coordinates;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.OutputType;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingReportStrategy;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingTransitionEvent;
import org.thingsboard.server.common.data.cf.configuration.geofencing.ZoneGroupConfiguration;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.TelemetryCalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntryType;
import org.thingsboard.server.service.cf.ctx.state.BaseCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LATITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.EntityCoordinates.ENTITY_ID_LONGITUDE_ARGUMENT_KEY;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.INSIDE;
import static org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus.OUTSIDE;

@Slf4j
@EqualsAndHashCode(callSuper = true)
public class GeofencingCalculatedFieldState extends BaseCalculatedFieldState implements ScheduledRefreshSupported {

    private long lastDynamicArgumentsRefreshTs = DEFAULT_LAST_UPDATE_TS;

    public GeofencingCalculatedFieldState(EntityId entityId) {
        super(entityId);
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.GEOFENCING;
    }

    @Override
    protected void validateNewEntry(String key, ArgumentEntry newEntry) {
        switch (key) {
            case ENTITY_ID_LATITUDE_ARGUMENT_KEY, ENTITY_ID_LONGITUDE_ARGUMENT_KEY -> {
                if (!(newEntry instanceof SingleValueArgumentEntry)) {
                    throw new IllegalArgumentException("Unsupported argument entry type for " + key + " argument: " + newEntry.getType() + ". " +
                                                       "Only SINGLE_VALUE type is allowed.");
                }
            }
            default -> {
                if (!(newEntry instanceof GeofencingArgumentEntry)) {
                    throw new IllegalArgumentException("Unsupported argument entry type for " + key + " argument: " + newEntry.getType() + ". " +
                                                       "Only GEOFENCING type is allowed.");
                }
            }
        }
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(Map<String, ArgumentEntry> updatedArgs, CalculatedFieldCtx ctx) {
        double latitude = (double) arguments.get(ENTITY_ID_LATITUDE_ARGUMENT_KEY).getValue();
        double longitude = (double) arguments.get(ENTITY_ID_LONGITUDE_ARGUMENT_KEY).getValue();
        Coordinates entityCoordinates = new Coordinates(latitude, longitude);

        var geofencingCfg = (GeofencingCalculatedFieldConfiguration) ctx.getCalculatedField().getConfiguration();
        Map<String, ZoneGroupConfiguration> zoneGroups = geofencingCfg.getZoneGroups();

        ObjectNode valuesNode = JacksonUtil.newObjectNode();
        List<ListenableFuture<Boolean>> relationFutures = new ArrayList<>();

        getGeofencingArguments().forEach((argumentKey, argumentEntry) -> {
            ZoneGroupConfiguration zoneGroupCfg = zoneGroups.get(argumentKey);
            if (zoneGroupCfg == null) {
                throw new RuntimeException("Zone group configuration is missing for the: " + entityId);
            }
            boolean createRelationsWithMatchedZones = zoneGroupCfg.isCreateRelationsWithMatchedZones();
            List<GeofencingEvalResult> zoneResults = new ArrayList<>(argumentEntry.getZoneStates().size());
            argumentEntry.getZoneStates().forEach((zoneId, zoneState) -> {
                boolean firstEval = zoneState.getLastPresence() == null;
                GeofencingEvalResult eval = zoneState.evaluate(entityCoordinates);
                zoneResults.add(eval);
                if (!createRelationsWithMatchedZones) {
                    return;
                }
                GeofencingTransitionEvent transitionEvent = eval.transition();
                if (transitionEvent == null && !firstEval) {
                    return;
                }
                transitionEvent = transitionEvent == null ? GeofencingTransitionEvent.LEFT : transitionEvent;
                EntityRelation relation = switch (zoneGroupCfg.getDirection()) {
                    case TO -> new EntityRelation(zoneId, entityId, zoneGroupCfg.getRelationType());
                    case FROM -> new EntityRelation(entityId, zoneId, zoneGroupCfg.getRelationType());
                };
                ListenableFuture<Boolean> f = switch (transitionEvent) {
                    case ENTERED -> ctx.getRelationService().saveRelationAsync(ctx.getTenantId(), relation);
                    case LEFT -> ctx.getRelationService().deleteRelationAsync(ctx.getTenantId(), relation);
                };
                relationFutures.add(f);
            });
            updateValuesNode(argumentKey, zoneResults, zoneGroupCfg.getReportStrategy(), valuesNode);
        });

        OutputType outputType = ctx.getOutput().getType();
        var result = TelemetryCalculatedFieldResult.builder()
                .outputStrategy(ctx.getOutput().getStrategy())
                .type(outputType)
                .scope(ctx.getOutput().getScope())
                .result(toResultNode(outputType, valuesNode))
                .build();
        if (relationFutures.isEmpty()) {
            return Futures.immediateFuture(result);
        }
        return Futures.whenAllComplete(relationFutures).call(() -> result, MoreExecutors.directExecutor());
    }

    @Override
    public void reset() {
        super.reset();
        resetScheduledRefreshTs();
    }

    @Override
    public void resetScheduledRefreshTs() {
        lastDynamicArgumentsRefreshTs = DEFAULT_LAST_UPDATE_TS;
    }

    @Override
    public long getLastScheduledRefreshTs() {
        return lastDynamicArgumentsRefreshTs;
    }

    @Override
    public void updateScheduledRefreshTs() {
        lastDynamicArgumentsRefreshTs = System.currentTimeMillis();
    }

    private Map<String, GeofencingArgumentEntry> getGeofencingArguments() {
        return arguments.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getType().equals(ArgumentEntryType.GEOFENCING))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (GeofencingArgumentEntry) entry.getValue()));
    }

    private void updateValuesNode(String argumentKey, List<GeofencingEvalResult> zoneResults, GeofencingReportStrategy geofencingReportStrategy, ObjectNode resultNode) {
        GeofencingEvalResult aggregationResult = aggregateZoneGroup(zoneResults);
        final String eventKey = argumentKey + "Event";
        final String statusKey = argumentKey + "Status";
        switch (geofencingReportStrategy) {
            case REPORT_TRANSITION_EVENTS_ONLY -> addTransitionEventIfExists(resultNode, aggregationResult, eventKey);
            case REPORT_PRESENCE_STATUS_ONLY -> resultNode.put(statusKey, aggregationResult.status().name());
            case REPORT_TRANSITION_EVENTS_AND_PRESENCE_STATUS -> {
                addTransitionEventIfExists(resultNode, aggregationResult, eventKey);
                resultNode.put(statusKey, aggregationResult.status().name());
            }
        }
    }

    private JsonNode toResultNode(OutputType outputType, ObjectNode valuesNode) {
        return toSimpleResult(outputType == OutputType.TIME_SERIES, valuesNode);
    }

    private GeofencingEvalResult aggregateZoneGroup(List<GeofencingEvalResult> zoneResults) {
        boolean nowInside = zoneResults.stream().anyMatch(r -> INSIDE.equals(r.status()));
        boolean prevInside = zoneResults.stream()
                .anyMatch(r -> GeofencingTransitionEvent.LEFT.equals(r.transition()) || r.transition() == null && r.status() == INSIDE);
        GeofencingTransitionEvent transition = null;
        if (!prevInside && nowInside) {
            transition = GeofencingTransitionEvent.ENTERED;
        } else if (prevInside && !nowInside) {
            transition = GeofencingTransitionEvent.LEFT;
        }
        return new GeofencingEvalResult(transition, nowInside ? INSIDE : OUTSIDE);
    }

    private void addTransitionEventIfExists(ObjectNode resultNode, GeofencingEvalResult aggregationResult, String eventKey) {
        if (aggregationResult.transition() != null) {
            resultNode.put(eventKey, aggregationResult.transition().name());
        }
    }

}
