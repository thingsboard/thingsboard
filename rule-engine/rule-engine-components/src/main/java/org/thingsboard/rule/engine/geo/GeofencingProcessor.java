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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class GeofencingProcessor {

    private final TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration;
    private final TbContext context;

    private static final String GEOFENCE_STATES_KEY = "geofenceStates";
    private static final String GEOFENCE_STATE_ATTRIBUTE_KEY_PREFIX = "geofenceState_%s";
    private static final String METADATA_DURATION_CONFIG_KEY = "durationConfig";

    public GeofencingProcessor(TbContext context, TbGpsMultiGeofencingActionNodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
        this.context = context;
    }

    public ListenableFuture<GeofenceResponse> process(RuleNodeId ruleNodeId, TbMsg msg, List<EntityId> matchedGeofences) {
        ListenableFuture<Set<GeofenceState>> geofenceStatesFuture = fetchGeofenceStatesForEntity(msg.getOriginator(), ruleNodeId);

        return Futures.transform(geofenceStatesFuture, geofenceStates -> processGeofenceStates(ruleNodeId, msg, matchedGeofences, geofenceStates), MoreExecutors.directExecutor());
    }

    private GeofenceResponse processGeofenceStates(RuleNodeId ruleNodeId, TbMsg msg, List<EntityId> matchedGeofences, Set<GeofenceState> geofenceStates) {
        if (isEmpty(geofenceStates) && isEmpty(matchedGeofences)) {
            return new GeofenceResponse();
        }

        geofenceStates = ensureNonNullGeofenceStates(geofenceStates);
        createNewGeofenceStates(geofenceStates, matchedGeofences);

        Optional<GeofenceDurationConfig> geofenceDurationConfig = getGeofenceDurationConfig(resolveDurationConfigKey(), msg);

        updateGeofenceStates(geofenceStates, geofenceDurationConfig, msg, matchedGeofences);

        GeofenceResponse geofenceResponse = new GeofenceResponse(getChangedStates(geofenceStates));

        Set<GeofenceState> aliveStates = getAliveStates(geofenceStates);
        persistState(msg.getOriginator(), ruleNodeId, aliveStates);

        return geofenceResponse;
    }

    private Set<GeofenceState> ensureNonNullGeofenceStates(Set<GeofenceState> geofenceStates) {
        return geofenceStates == null ? new HashSet<>() : geofenceStates;
    }

    private void updateGeofenceStates(Set<GeofenceState> geofenceStates, Optional<GeofenceDurationConfig> geofenceDurationConfig, TbMsg msg, List<EntityId> matchedGeofences) {
        geofenceStates.forEach(geofenceState -> geofenceState.updateStatus(nodeConfiguration, geofenceDurationConfig, msg, matchedGeofences));
    }

    private Set<GeofenceState> getChangedStates(Set<GeofenceState> geofenceStates) {
        return geofenceStates.stream()
                .filter(GeofenceState::isStatusChanged)
                .collect(Collectors.toSet());
    }

    private Set<GeofenceState> getAliveStates(Set<GeofenceState> geofenceStates) {
        return geofenceStates.stream()
                .filter(geofenceState -> !geofenceState.isStatus(GeofenceStateStatus.OUTSIDE))
                .collect(Collectors.toSet());
    }

    private void persistState(EntityId entityId, RuleNodeId ruleNodeId, Set<GeofenceState> geofenceStates) {
        ObjectNode jsonNode = JacksonUtil.newObjectNode();

        jsonNode.set(GEOFENCE_STATES_KEY, JacksonUtil.valueToTree(geofenceStates));

        BaseAttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(new JsonDataEntry(getGeofenceStateAttributeKey(ruleNodeId), jsonNode.toString()), System.currentTimeMillis());
        context.getAttributesService().save(context.getTenantId(), entityId, AttributeScope.SERVER_SCOPE, attributeKvEntry);
    }

    private ListenableFuture<Set<GeofenceState>> fetchGeofenceStatesForEntity(EntityId entityId, RuleNodeId ruleNodeId) {
        ListenableFuture<Optional<AttributeKvEntry>> optionalAttributeFuture = context.getAttributesService().find(context.getTenantId(), entityId, AttributeScope.SERVER_SCOPE, getGeofenceStateAttributeKey(ruleNodeId));
        return Futures.transform(optionalAttributeFuture, optionalAttribute -> {
            if (optionalAttribute.isEmpty() || optionalAttribute.get().getJsonValue().isEmpty()) {
                return null;
            } else {
                JsonNode jsonNode = JacksonUtil.toJsonNode(optionalAttribute.get().getJsonValue().get());
                return JacksonUtil.convertValue(jsonNode.get(GEOFENCE_STATES_KEY), new TypeReference<>() {
                });
            }
        }, MoreExecutors.directExecutor());
    }

    private String getGeofenceStateAttributeKey(RuleNodeId ruleNodeId) {
        return String.format(GEOFENCE_STATE_ATTRIBUTE_KEY_PREFIX, ruleNodeId.getId().toString());
    }

    private String resolveDurationConfigKey() {
        return Optional.ofNullable(nodeConfiguration.getMetadataDurationConfigKey())
                .filter(StringUtils::isNotEmpty)
                .orElse(METADATA_DURATION_CONFIG_KEY);
    }

    private Optional<GeofenceDurationConfig> getGeofenceDurationConfig(String durationConfigMetadataKey, TbMsg msg) {
        Map<UUID, GeofenceDuration> geofenceDurationMap = JacksonUtil.fromString(msg.getMetaData().getValue(durationConfigMetadataKey), new TypeReference<>() {
        });
        if (geofenceDurationMap == null) {
            return Optional.empty();
        }
        return Optional.of(new GeofenceDurationConfig(geofenceDurationMap));
    }

    private void createNewGeofenceStates(Set<GeofenceState> geofenceStates, List<EntityId> matchedGeofences) {
        Set<EntityId> existingGeofenceIds = geofenceStates.stream()
                .map(GeofenceState::getGeofenceId)
                .collect(Collectors.toSet());

        matchedGeofences.stream()
                .filter(matchedGeofence -> !existingGeofenceIds.contains(matchedGeofence))
                .map(GeofenceState::new)
                .forEach(geofenceStates::add);
    }

}
