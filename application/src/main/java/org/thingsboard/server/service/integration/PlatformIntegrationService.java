// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.gen.integration.TbIntegrationTsDataProto;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceActivityProto;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;

import java.util.UUID;

public interface PlatformIntegrationService {

    Runnable processUplinkData(AbstractIntegration info, DeviceUplinkDataProto data, IntegrationCallback<Void> callback);

    Runnable processUplinkData(AbstractIntegration info, UUID sessionId, DeviceUplinkDataProto data, IntegrationCallback<Void> callback);

    Runnable processUplinkData(AbstractIntegration info, AssetUplinkDataProto data, IntegrationCallback<Void> callback);

    Runnable processUplinkData(AbstractIntegration info, EntityViewDataProto data, IntegrationCallback<Void> callback);

    void processUplinkData(AbstractIntegration info, TbMsg data, IntegrationApiCallback callback);

    void processUplinkData(TbIntegrationEventProto data, IntegrationApiCallback callback);

    void processUplinkData(TbIntegrationTsDataProto data, IntegrationApiCallback callback);

    void processDeviceActivityData(DeviceActivityProto data, IntegrationApiCallback callback);

    void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, IntegrationCallback<Void> callback);

    void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, IntegrationCallback<Void> callback);

    void process(TenantId tenantId, TbMsg tbMsg, IntegrationCallback<Void> callback);

    Device processGetOrCreateDevice(AbstractIntegration integration, String name, String type, String label, String customerName, String groupName);

    Asset processGetOrCreateAsset(AbstractIntegration integration, String name, String type, String label, String customerName, String groupName);

}
