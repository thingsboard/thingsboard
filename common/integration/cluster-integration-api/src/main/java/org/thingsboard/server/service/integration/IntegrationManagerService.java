// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.integration.IntegrationValidationRequestProto;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationDownlinkMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationValidationResponseProto;

import java.util.Set;

public interface IntegrationManagerService {

    ListenableFuture<Void> validateIntegrationConfiguration(Integration integration);

    ListenableFuture<Void> checkIntegrationConnection(Integration integration);

    ListenableFuture<ThingsboardPlatformIntegration> getIntegrationByRoutingKey(String key);

    void refresh(IntegrationType integrationType, Set<TopicPartitionInfo> newPartitions);

    void handleComponentLifecycleMsg(ComponentLifecycleMsg componentLifecycleMsg);

    void handleDownlink(IntegrationDownlinkMsgProto downlinkMsg, TbCallback callback);

    void handleValidationRequest(IntegrationValidationRequestProto validationRequestMsg, TbCallback callback);

    void handleValidationResponse(IntegrationValidationResponseProto validationRequestMsg, TbCallback callback);

    int getIntegrationConnectionCheckApiRequestTimeoutSec();

}
