/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.geo;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.springframework.util.CollectionUtils.isEmpty;

public class GeofencingProcessor {

    private final TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration;
    private final TbContext context;

    private static final String GEOFENCE_STATES_KEY = "geofenceStates";
    private static final String GEOFENCE_STATE_ATTRIBUTE_KEY_PREFIX = "geofenceState_%s";

    public GeofencingProcessor(TbContext context, TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
        this.context = context;
    }

    public ListenableFuture<GeofenceResponse> process(Long ts, RuleNodeId ruleNodeId, EntityId entityId, List<EntityId> matchedGeofences) {
        ListenableFuture<Set<GeofenceState>> geofenceStatesFuture = fetchGeofenceStatesForEntity(entityId, ruleNodeId);

        return Futures.transform(geofenceStatesFuture, geofenceStates -> {
            List<EntityId> enteredGeofences = null;
            List<EntityId> leftGeofences = null;
            List<EntityId> insideGeofences = null;

            if (isEmpty(matchedGeofences)) {
                leftGeofences = getLeftGeofences(ts, emptyList(), geofenceStates);
            } else if (hasEnteredGeofence(matchedGeofences, geofenceStates)) {
                insideGeofences = getInsideGeofences(ts, nodeConfiguration.getInsideDurationMs(), matchedGeofences, geofenceStates);
            } else if (hasEnteredNotMatchedGeofence(matchedGeofences, geofenceStates)) {
                leftGeofences = getLeftGeofences(ts, matchedGeofences, geofenceStates);
                enteredGeofences = getEnteredGeofences(ts, matchedGeofences, geofenceStates);
            } else if (hasNotEnteredAnyGeofence(geofenceStates)) {
                enteredGeofences = getEnteredGeofences(ts, matchedGeofences, geofenceStates);
            }

            List<EntityId> outsideGeofences = getOutsideGeofences(ts, nodeConfiguration.getOutsideDurationMs(), matchedGeofences, geofenceStates);

            persistState(entityId, ruleNodeId, geofenceStates);

            return new GeofenceResponse(enteredGeofences, leftGeofences, insideGeofences, outsideGeofences);
        }, MoreExecutors.directExecutor());
    }

    private List<EntityId> getEnteredGeofences(Long currentTs, List<EntityId> matchedGeofences, Set<GeofenceState> geofenceStates) {
        if (isEmpty(matchedGeofences)) {
            return emptyList();
        }

        Optional<GeofenceState> enteredGeofenceState = geofenceStates.stream().filter(GeofenceState::isEntered).findFirst();

        if (enteredGeofenceState.isPresent()) {
            return emptyList();
        }

        deleteGeofenceStateByGeofenceId(matchedGeofences.get(0), geofenceStates);

        GeofenceState geofenceState = new GeofenceState(matchedGeofences.get(0), currentTs);

        geofenceStates.add(geofenceState);

        return List.of(geofenceState.getGeofenceId());
    }

    private List<EntityId> getLeftGeofences(Long currentTs, List<EntityId> matchedGeofences, Set<GeofenceState> geofenceStates) {
        Optional<GeofenceState> optionalGeofenceState = geofenceStates.stream()
                .filter(state -> state.isEntered() && !matchedGeofences.contains(state.getGeofenceId()))
                .findFirst();

        if (optionalGeofenceState.isEmpty()) {
            return emptyList();
        }

        GeofenceState geofenceState = optionalGeofenceState.get();
        geofenceState.setLeftTs(currentTs);

        return List.of(geofenceState.getGeofenceId());
    }

    private List<EntityId> getInsideGeofences(Long currentTs, Long durationMs, List<EntityId> matchedGeofences, Set<GeofenceState> geofenceStates) {
        Optional<GeofenceState> optionalGeofenceState = geofenceStates.stream().filter(state -> state.isEntered() && state.getInsideTs() == null && matchedGeofences.contains(state.getGeofenceId())).findFirst();

        if (optionalGeofenceState.isEmpty()) {
            return emptyList();
        }

        GeofenceState geofenceState = optionalGeofenceState.get();

        long diff = currentTs - geofenceState.getEnterTs();
        boolean isOver = diff > durationMs;
        if (isOver) {
            geofenceState.setInsideTs(currentTs);
            return List.of(geofenceState.getGeofenceId());
        }

        return emptyList();
    }

    private List<EntityId> getOutsideGeofences(Long currentTs, Long durationMs, List<EntityId> matchedGeofences, Set<GeofenceState> geofenceStates) {
        Set<GeofenceState> candidatesForOutside = geofenceStates.stream().filter(state -> state.isLeft() && !matchedGeofences.contains(state.getGeofenceId())).collect(Collectors.toSet());

        if (isEmpty(candidatesForOutside)) {
            return emptyList();
        }

        List<GeofenceState> outsideGeofences = new ArrayList<>();

        for (GeofenceState candidate : candidatesForOutside) {
            long diff = currentTs - candidate.getLeftTs();
            boolean isTimeForOutside = diff > durationMs;
            if (isTimeForOutside) {
                outsideGeofences.add(candidate);
            }
        }

        outsideGeofences.forEach(geofenceStates::remove);

        return outsideGeofences.stream().map(GeofenceState::getGeofenceId).collect(Collectors.toList());
    }

    private boolean hasEnteredGeofence(List<EntityId> matchedGeofences, Set<GeofenceState> geofenceStates) {
        return geofenceStates.stream().anyMatch(geofenceState -> geofenceState.isEntered() && matchedGeofences.contains(geofenceState.getGeofenceId()));
    }

    private boolean hasEnteredNotMatchedGeofence(List<EntityId> matchedGeofences, Set<GeofenceState> geofenceStates) {
        return geofenceStates.stream().anyMatch(geofenceState -> geofenceState.isEntered() && !matchedGeofences.contains(geofenceState.getGeofenceId()));
    }

    private boolean hasNotEnteredAnyGeofence(Set<GeofenceState> geofenceStates) {
        return geofenceStates.stream().allMatch(GeofenceState::isLeft);
    }

    private void persistState(EntityId entityId, RuleNodeId ruleNodeId, Set<GeofenceState> geofenceStates) {
        ObjectNode jsonNode = JacksonUtil.newObjectNode();

        jsonNode.set(GEOFENCE_STATES_KEY, JacksonUtil.valueToTree(geofenceStates));

        BaseAttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(new JsonDataEntry(getGeofenceStateAttributeKey(ruleNodeId), jsonNode.toString()), System.currentTimeMillis());
        context.getAttributesService().save(context.getTenantId(), entityId, AttributeScope.SERVER_SCOPE, attributeKvEntry);
    }

    private void deleteGeofenceStateByGeofenceId(EntityId geofenceId, Set<GeofenceState> geofenceStates) {
        Optional<GeofenceState> geofenceState = geofenceStates.stream().filter(state -> state.getGeofenceId().equals(geofenceId)).findFirst();
        geofenceState.ifPresent(geofenceStates::remove);
    }

    private ListenableFuture<Set<GeofenceState>> fetchGeofenceStatesForEntity(EntityId entityId, RuleNodeId ruleNodeId) {
        ListenableFuture<Optional<AttributeKvEntry>> optionalAttributeFuture = context.getAttributesService().find(context.getTenantId(), entityId, AttributeScope.SERVER_SCOPE, getGeofenceStateAttributeKey(ruleNodeId));
        return Futures.transform(optionalAttributeFuture, optionalAttribute -> {
            if (optionalAttribute.isEmpty() || optionalAttribute.get().getJsonValue().isEmpty()) {
                return new HashSet<>();
            } else {
                JsonNode jsonNode = JacksonUtil.toJsonNode(optionalAttribute.get().getJsonValue().get());
                Set<GeofenceState> geofenceStates = JacksonUtil.convertValue(jsonNode.get(GEOFENCE_STATES_KEY), new TypeReference<>() {
                });
                return geofenceStates == null ? new HashSet<>() : geofenceStates;
            }
        }, MoreExecutors.directExecutor());
    }

    private String getGeofenceStateAttributeKey(RuleNodeId ruleNodeId) {
        return String.format(GEOFENCE_STATE_ATTRIBUTE_KEY_PREFIX, ruleNodeId.getId().toString());
    }

}
