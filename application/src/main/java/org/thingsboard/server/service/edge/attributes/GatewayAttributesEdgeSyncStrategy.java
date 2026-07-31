/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.attributes;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.service.gateway_device.GatewayFlagCache;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Pushes attribute updates of gateway devices to the edges the gateway is assigned to.
 * <p>
 * Gateway configuration (connectors, remote logging level, etc.) is stored in attributes of the gateway device.
 * This strategy converts such updates into edge notifications that are processed asynchronously
 * by the edge notification queue consumer (see DeviceEdgeProcessor).
 * <p>
 * SHARED_SCOPE attributes are propagated. SERVER_SCOPE attributes are propagated except the device activity keys:
 * configuration of the disabled connectors is stored in SERVER_SCOPE attributes named after the connector,
 * so the keys can not be limited to a predefined set. This matches the behavior of the attributes sync
 * on the edge assignment/reconnect (see DefaultEdgeRequestsService). 'inactivityTimeout' is deliberately
 * propagated (not treated as an activity key) so that it applies to the device state tracked on the edge,
 * where the gateway is actually connected. CLIENT_SCOPE attributes are reported by the gateway itself
 * and reach the cloud via the edge uplink, so there is nothing to propagate back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayAttributesEdgeSyncStrategy implements AttributesEdgeSyncStrategy {

    private final TbClusterService clusterService;
    private final GatewayFlagCache gatewayFlagCache;

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

    @Override
    public boolean isEdgeSyncRequired(AttributesSaveRequest request) {
        // key based checks are free and short-circuit the frequent writes (e.g. device activity state);
        // the gateway flag check is in-memory only - the flag is resolved by the async part of the sync
        return hasGatewaySyncSupportedAttributes(request.getScope(), request.getEntries(), AttributeKvEntry::getKey)
                && gatewayFlagCache.isPotentialGateway(request.getEntityId());
    }

    @Override
    public boolean isEdgeSyncRequired(AttributesDeleteRequest request) {
        return hasGatewaySyncSupportedAttributes(request.getScope(), request.getKeys(), key -> key)
                && gatewayFlagCache.isPotentialGateway(request.getEntityId());
    }

    @Override
    public void onAttributesUpdate(AttributesSaveRequest request) {
        TenantId tenantId = request.getTenantId();
        DeviceId deviceId = new DeviceId(request.getEntityId().getId());
        // executed on the sync pool only: the gateway flag resolution may be blocking
        if (!gatewayFlagCache.isGateway(tenantId, deviceId)) {
            return;
        }
        List<AttributeKvEntry> entries = filterGatewayAttributes(request.getScope(), request.getEntries(), AttributeKvEntry::getKey);
        Map<String, Object> body = new HashMap<>();
        body.put("kv", toJson(entries));
        body.put("ts", entries.stream().mapToLong(AttributeKvEntry::getLastUpdateTs).max().orElseGet(System::currentTimeMillis));
        body.put(DataConstants.SCOPE, request.getScope().name());
        log.debug("[{}][{}] Pushing gateway attributes update to edge, scope [{}], body [{}]", tenantId, deviceId, request.getScope(), body);
        clusterService.sendNotificationMsgToEdge(tenantId, null, deviceId, JacksonUtil.toString(body),
                EdgeEventType.DEVICE, EdgeEventActionType.ATTRIBUTES_UPDATED, null);
    }

    @Override
    public void onAttributesDelete(AttributesDeleteRequest request) {
        TenantId tenantId = request.getTenantId();
        DeviceId deviceId = new DeviceId(request.getEntityId().getId());
        // executed on the sync pool only: the gateway flag resolution may be blocking
        if (!gatewayFlagCache.isGateway(tenantId, deviceId)) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("keys", filterGatewayAttributes(request.getScope(), request.getKeys(), key -> key));
        body.put(DataConstants.SCOPE, request.getScope().name());
        log.debug("[{}][{}] Pushing gateway attributes delete to edge, scope [{}], body [{}]", tenantId, deviceId, request.getScope(), body);
        clusterService.sendNotificationMsgToEdge(tenantId, null, deviceId, JacksonUtil.toString(body),
                EdgeEventType.DEVICE, EdgeEventActionType.ATTRIBUTES_DELETED, null);
    }

    private static <T> boolean hasGatewaySyncSupportedAttributes(AttributeScope scope, List<T> entries, Function<T, String> keyExtractor) {
        return switch (scope) {
            case SHARED_SCOPE -> !entries.isEmpty();
            case SERVER_SCOPE -> entries.stream().anyMatch(entry -> !isActivityKey(keyExtractor.apply(entry)));
            case CLIENT_SCOPE -> false;
        };
    }

    private static <T> List<T> filterGatewayAttributes(AttributeScope scope, List<T> entries, Function<T, String> keyExtractor) {
        if (scope == AttributeScope.SERVER_SCOPE) {
            return entries.stream().filter(entry -> !isActivityKey(keyExtractor.apply(entry))).toList();
        }
        return entries;
    }

    private static boolean isActivityKey(String key) {
        return DefaultDeviceStateService.ACTIVITY_KEYS_WITHOUT_INACTIVITY_TIMEOUT.contains(key);
    }

    private static ObjectNode toJson(List<AttributeKvEntry> entries) {
        ObjectNode attributes = JacksonUtil.newObjectNode();
        entries.forEach(entry -> JacksonUtil.addKvEntry(attributes, entry));
        return attributes;
    }

}
