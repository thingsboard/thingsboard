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
package org.thingsboard.server.cluster;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.EdgeHighPriorityMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.RestApiCallResponseMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueClusterService;

import java.util.UUID;

public interface TbClusterService extends TbQueueClusterService {

    void pushMsgToCore(TopicPartitionInfo tpi, UUID msgKey, ToCoreMsg msg, TbQueueCallback callback);

    void pushMsgToCore(TenantId tenantId, EntityId entityId, ToCoreMsg msg, TbQueueCallback callback);

    void pushMsgToCore(ToDeviceActorNotificationMsg msg, TbQueueCallback callback);

    void broadcastToCore(ToCoreNotificationMsg msg);

    void broadcastToCalculatedFields(ToCalculatedFieldNotificationMsg build, TbQueueCallback callback);

    void pushMsgToVersionControl(TenantId tenantId, ToVersionControlServiceMsg msg, TbQueueCallback callback);

    void pushNotificationToCore(String targetServiceId, FromDeviceRpcResponse response, TbQueueCallback callback);

    void pushNotificationToCore(String targetServiceId, RestApiCallResponseMsgProto msg, TbQueueCallback callback);

    void pushMsgToRuleEngine(TopicPartitionInfo tpi, UUID msgId, ToRuleEngineMsg msg, TbQueueCallback callback);

    void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg msg, TbQueueCallback callback);

    void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg msg, boolean useQueueFromTbMsg, TbQueueCallback callback);

    void pushNotificationToRuleEngine(String targetServiceId, FromDeviceRpcResponse response, TbQueueCallback callback);

    void pushNotificationToTransport(String targetServiceId, ToTransportMsg response, TbQueueCallback callback);

    void pushMsgToCalculatedFields(TenantId tenantId, EntityId entityId, TransportProtos.ToCalculatedFieldMsg msg, TbQueueCallback callback);

    void pushMsgToCalculatedFields(TopicPartitionInfo tpi, UUID msgId, ToCalculatedFieldMsg msg, TbQueueCallback callback);

    void broadcastEntityStateChangeEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state);

    void broadcast(ComponentLifecycleMsg componentLifecycleMsg);

    void onDeviceProfileChange(DeviceProfile deviceProfile, DeviceProfile oldDeviceProfile, TbQueueCallback callback);

    void onDeviceProfileDelete(DeviceProfile deviceProfile, TbQueueCallback callback);

    void onTenantProfileChange(TenantProfile tenantProfile, TbQueueCallback callback);

    void onTenantProfileDelete(TenantProfile tenantProfile, TbQueueCallback callback);

    void onTenantChange(Tenant tenant, TbQueueCallback callback);

    void onTenantDelete(Tenant tenant, TbQueueCallback callback);

    void onApiStateChange(ApiUsageState apiUsageState, TbQueueCallback callback);

    void onDeviceUpdated(Device device, Device old);

    void onDeviceDeleted(TenantId tenantId, Device device, TbQueueCallback callback);

    void onDeviceAssignedToTenant(TenantId oldTenantId, Device device);

    void onAssetUpdated(Asset asset, Asset old);

    void onAssetDeleted(TenantId tenantId, Asset asset, TbQueueCallback callback);

    void onResourceChange(TbResourceInfo resource, TbQueueCallback callback);

    void onResourceDeleted(TbResourceInfo resource, TbQueueCallback callback);

    void onEdgeHighPriorityMsg(EdgeHighPriorityMsg msg);

    void onEdgeEventUpdate(EdgeEventUpdateMsg msg);

    void onEdgeStateChangeEvent(ComponentLifecycleMsg msg);

    void pushEdgeSyncRequestToEdge(ToEdgeSyncRequest request);

    void pushEdgeSyncResponseToCore(FromEdgeSyncResponse response, String requestServiceId);

    void pushMsgToEdge(TenantId tenantId, EntityId entityId, ToEdgeMsg msg, TbQueueCallback callback);

    void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body, EdgeEventType type, EdgeEventActionType action, EdgeId sourceEdgeId);

    void onCalculatedFieldUpdated(CalculatedField calculatedField, CalculatedField oldCalculatedField, TbQueueCallback callback);

    void onCalculatedFieldDeleted(CalculatedField calculatedField, TbQueueCallback callback);

}
