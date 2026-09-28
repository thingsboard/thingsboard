// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.service.api;

import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.gen.integration.TbIntegrationTsDataProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceActivityProto;
import org.thingsboard.server.gen.transport.TransportProtos.GetQueueRoutingInfoResponseMsg;

import java.util.List;

public interface IntegrationApiService {

    void sendUplinkData(Integration integration, IntegrationInfoProto integrationInfoProto, DeviceUplinkDataProto data, IntegrationCallback<Void> callback);

    void sendUplinkData(Integration integration, IntegrationInfoProto integrationInfoProto, AssetUplinkDataProto data, IntegrationCallback<Void> callback);

    void sendUplinkData(Integration integration, IntegrationInfoProto integrationInfoProto, TbMsg data, IntegrationCallback<Void> callback);

    void sendEventData(TenantId tenantId, EntityId entityId, TbIntegrationEventProto data, IntegrationCallback<Void> callback);

    void sendTsData(TenantId tenantId, EntityId id, TbIntegrationTsDataProto tsData, IntegrationCallback<Void> callback);

    void sendDeviceActivityData(Integration integration, IntegrationInfoProto proto, DeviceActivityProto data, IntegrationCallback<Void> callback);

    List<GetQueueRoutingInfoResponseMsg> getQueueRoutingInfo(TransportProtos.GetAllQueueRoutingInfoRequestMsg msg);

}
