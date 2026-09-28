// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.DefaultIntegrationDownlinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.transport.TransportProtos.IntegrationDownlinkMsgProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.settings.TbQueueIntegrationExecutorSettings;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultTbIntegrationDownlinkService implements TbIntegrationDownlinkService {

    private final PartitionService partitionService;
    private final IntegrationService integrationService;
    private final RemoteIntegrationRpcService remoteRpcService;
    private final TbQueueProducerProvider producerProvider;
    private final TbQueueIntegrationExecutorSettings integrationExecutorSettings;
    private final TopicService topicService;

    @Override
    public void onRuleEngineDownlinkMsg(TenantId tenantId, IntegrationId integrationId, IntegrationDownlinkMsgProto downlinkMsg, TbCallback callback) {
        Integration integration = integrationService.findIntegrationById(tenantId, integrationId);
        if (integration == null) {
            callback.onFailure(new TbNodeException("Integration is missing!"));
        } else if (!integration.isEnabled()) {
            callback.onFailure(new TbNodeException("Integration is disabled!"));
        } else if (integration.isRemote()) {
            onDownlinkToRemoteIntegrationMsg(tenantId, integrationId, downlinkMsg);
            callback.onSuccess();
        } else {
            var producer = producerProvider.getTbIntegrationExecutorDownlinkMsgProducer();
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_INTEGRATION_EXECUTOR, integration.getType().name(), tenantId, integrationId)
                    .withTopic(topicService.buildTopicName(integrationExecutorSettings.getIntegrationDownlinkTopic(integration.getType())));
            producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), ToIntegrationExecutorDownlinkMsg.newBuilder().setDownlinkMsg(downlinkMsg).build()), new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    callback.onSuccess();
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onFailure(t);
                }
            });
        }
    }

    @Override
    public void onDownlinkToRemoteIntegrationMsg(IntegrationDownlinkMsgProto msgProto, TbCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(msgProto.getTenantIdMSB(), msgProto.getTenantIdLSB()));
        IntegrationId integrationId = new IntegrationId(new UUID(msgProto.getIntegrationIdMSB(), msgProto.getIntegrationIdLSB()));
        onDownlinkToRemoteIntegrationMsg(tenantId, integrationId, msgProto);
        callback.onSuccess();
    }

    private void onDownlinkToRemoteIntegrationMsg(TenantId tenantId, IntegrationId integrationId, IntegrationDownlinkMsgProto downlinkMsg) {
        IntegrationDownlinkMsg msg = new DefaultIntegrationDownlinkMsg(tenantId, integrationId,
                TbMsg.fromProto(null, downlinkMsg.getDataProto(), TbMsgCallback.EMPTY), null);
        remoteRpcService.handleRemoteDownlink(msg);
    }

}
