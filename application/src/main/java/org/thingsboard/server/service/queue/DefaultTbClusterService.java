/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.queue;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.FromDeviceRPCResponseProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.encoding.DataDecodingEncodingService;
import org.thingsboard.server.service.rpc.FromDeviceRpcResponse;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DefaultTbClusterService implements TbClusterService {

    @Value("${cluster.stats.enabled:false}")
    private boolean statsEnabled;

    private final AtomicInteger toCoreMsgs = new AtomicInteger(0);
    private final AtomicInteger toCoreNfs = new AtomicInteger(0);
    private final AtomicInteger toRuleEngineMsgs = new AtomicInteger(0);
    private final AtomicInteger toRuleEngineNfs = new AtomicInteger(0);
    private final AtomicInteger toTransportNfs = new AtomicInteger(0);

    private final TbQueueProducerProvider producerProvider;
    private final PartitionService partitionService;
    private final NotificationsTopicService notificationsTopicService;
    private final DataDecodingEncodingService encodingService;

    public DefaultTbClusterService(TbQueueProducerProvider producerProvider,
                                   PartitionService partitionService,
                                   DataDecodingEncodingService encodingService,
                                   NotificationsTopicService notificationsTopicService) {
        this.producerProvider = producerProvider;
        this.partitionService = partitionService;
        this.encodingService = encodingService;
        this.notificationsTopicService = notificationsTopicService;
    }

    @Override
    public void pushMsgToCore(TenantId tenantId, EntityId entityId, ToCoreMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToCore(TopicPartitionInfo tpi, UUID msgId, ToCoreMsg msg, TbQueueCallback callback) {
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(msgId, msg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToCore(ToDeviceActorNotificationMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, msg.getTenantId(), msg.getDeviceId());
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        byte[] msgBytes = encodingService.encode(msg);
        ToCoreMsg toCoreMsg = ToCoreMsg.newBuilder().setToDeviceActorNotificationMsg(ByteString.copyFrom(msgBytes)).build();
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(msg.getDeviceId().getId(), toCoreMsg), callback);
        toCoreMsgs.incrementAndGet();
    }

    @Override
    public void pushNotificationToCore(String serviceId, FromDeviceRpcResponse response, TbQueueCallback callback) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        FromDeviceRPCResponseProto.Builder builder = FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(response.getId().getMostSignificantBits())
                .setRequestIdLSB(response.getId().getLeastSignificantBits())
                .setError(response.getError().isPresent() ? response.getError().get().ordinal() : -1);
        response.getResponse().ifPresent(builder::setResponse);
        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setFromDeviceRpcResponse(builder).build();
        producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(response.getId(), msg), callback);
        toCoreNfs.incrementAndGet();
    }

    @Override
    public void pushMsgToRuleEngine(TopicPartitionInfo tpi, UUID msgId, ToRuleEngineMsg msg, TbQueueCallback callback) {
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        producerProvider.getRuleEngineMsgProducer().send(tpi, new TbProtoQueueMsg<>(msgId, msg), callback);
        toRuleEngineMsgs.incrementAndGet();
    }

    @Override
    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg tbMsg, TbQueueCallback callback) {
        if (tenantId.isNullUid()) {
            if (entityId.getEntityType().equals(EntityType.TENANT)) {
                tenantId = new TenantId(entityId.getId());
            } else {
                log.warn("[{}][{}] Received invalid message: {}", tenantId, entityId, tbMsg);
                return;
            }
        }
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, entityId);
        log.trace("PUSHING msg: {} to:{}", tbMsg, tpi);
        ToRuleEngineMsg msg = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg)).build();
        producerProvider.getRuleEngineMsgProducer().send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
        toRuleEngineMsgs.incrementAndGet();
    }

    @Override
    public void pushNotificationToRuleEngine(String serviceId, FromDeviceRpcResponse response, TbQueueCallback callback) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        FromDeviceRPCResponseProto.Builder builder = FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(response.getId().getMostSignificantBits())
                .setRequestIdLSB(response.getId().getLeastSignificantBits())
                .setError(response.getError().isPresent() ? response.getError().get().ordinal() : -1);
        response.getResponse().ifPresent(builder::setResponse);
        ToRuleEngineNotificationMsg msg = ToRuleEngineNotificationMsg.newBuilder().setFromDeviceRpcResponse(builder).build();
        producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(response.getId(), msg), callback);
        toRuleEngineNfs.incrementAndGet();
    }

    @Override
    public void pushNotificationToTransport(String serviceId, ToTransportMsg response, TbQueueCallback callback) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), response), callback);
        toTransportNfs.incrementAndGet();
    }

    @Override
    public void onEntityStateChange(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state) {
        log.trace("[{}] Processing {} state change event: {}", tenantId, entityId.getEntityType(), state);
        broadcast(new ComponentLifecycleMsg(tenantId, entityId, state));
    }

    @Override
    public void onQueueChange(Queue queue, TbQueueCallback callback) {
        log.trace("[{}][{}] Processing queue change [{}] event", queue.getTenantId(), queue.getId(), queue.getName());

        TransportProtos.QueueUpdateMsg queueUpdateMsg = TransportProtos.QueueUpdateMsg.newBuilder()
                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                .setQueueName(queue.getName())
                .setQueueTopic(queue.getTopic())
                .setPartitions(queue.getPartitions())
                .build();

        if ("Main".equals(queue.getName())) {
            Set<TransportProtos.ServiceInfo> tbTransportServices = partitionService.getAllServices(ServiceType.TB_TRANSPORT);
            ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
            for (TransportProtos.ServiceInfo transportService : tbTransportServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transportService.getServiceId());
                producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), transportMsg), callback);
                toTransportNfs.incrementAndGet();
            }

            Set<TransportProtos.ServiceInfo> tbCoreServices = partitionService.getAllServices(ServiceType.TB_CORE);
            ToCoreNotificationMsg coreMsg = ToCoreNotificationMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
            for (TransportProtos.ServiceInfo coreService : tbCoreServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, coreService.getServiceId());
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), coreMsg), callback);
                toCoreNfs.incrementAndGet();
            }
        } else {
            Set<TransportProtos.ServiceInfo> tbRuleEngineServices = partitionService.getAllServices(ServiceType.TB_RULE_ENGINE);
            ToRuleEngineNotificationMsg ruleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setQueueUpdateMsg(queueUpdateMsg).build();
            for (TransportProtos.ServiceInfo ruleEngineService : tbRuleEngineServices) {
                TenantId tenantId = new TenantId(new UUID(ruleEngineService.getTenantIdMSB(), ruleEngineService.getTenantIdLSB()));
                if (tenantId.equals(queue.getTenantId())) {
                    TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngineService.getServiceId());
                    producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), ruleEngineMsg), callback);
                    toRuleEngineNfs.incrementAndGet();
                }
            }
        }
    }

    @Override
    public void onQueueDelete(Queue queue, TbQueueCallback callback) {
        log.trace("[{}][{}] Processing queue delete [{}] event", queue.getTenantId(), queue.getId(), queue.getName());

        TransportProtos.QueueDeleteMsg queueDeleteMsg = TransportProtos.QueueDeleteMsg.newBuilder()
                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                .setQueueName(queue.getName())
                .build();

        if ("Main".equals(queue.getName())) {
            Set<TransportProtos.ServiceInfo> tbTransportServices = partitionService.getAllServices(ServiceType.TB_TRANSPORT);
            ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
            for (TransportProtos.ServiceInfo transportService : tbTransportServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transportService.getServiceId());
                producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), transportMsg), callback);
                toTransportNfs.incrementAndGet();
            }

            Set<TransportProtos.ServiceInfo> tbCoreServices = partitionService.getAllServices(ServiceType.TB_CORE);
            ToCoreNotificationMsg coreMsg = ToCoreNotificationMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
            for (TransportProtos.ServiceInfo coreService : tbCoreServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, coreService.getServiceId());
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), coreMsg), callback);
                toCoreNfs.incrementAndGet();
            }
        } else {
            //TODO: should be changed with smth like TransportApiRequestTemplate and get responses from all RE's
            Set<TransportProtos.ServiceInfo> tbRuleEngineServices = partitionService.getAllServices(ServiceType.TB_RULE_ENGINE);
            ToRuleEngineNotificationMsg ruleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setQueueDeleteMsg(queueDeleteMsg).build();
            for (TransportProtos.ServiceInfo ruleEngineService : tbRuleEngineServices) {
                TenantId tenantId = new TenantId(new UUID(ruleEngineService.getTenantIdMSB(), ruleEngineService.getTenantIdLSB()));
                if (tenantId.equals(queue.getTenantId())) {
                    TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngineService.getServiceId());
                    log.info("Send notification about deleting queue [{}] to Rule Engine [{}]", queue.getName(), tpi.getFullTopicName());
                    producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), ruleEngineMsg), callback);
                    toRuleEngineNfs.incrementAndGet();
                }
            }
            //TODO: should be removed!!!
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(ComponentLifecycleMsg msg) {
        byte[] msgBytes = encodingService.encode(msg);
        TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> toRuleEngineProducer = producerProvider.getRuleEngineNotificationsMsgProducer();
        Set<TransportProtos.ServiceInfo> tbRuleEngineServices = new HashSet<>(partitionService.getAllServices(ServiceType.TB_RULE_ENGINE));
        if (msg.getEntityId().getEntityType().equals(EntityType.TENANT)) {
            TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNfProducer = producerProvider.getTbCoreNotificationsMsgProducer();
            Set<TransportProtos.ServiceInfo> tbCoreServices = partitionService.getAllServices(ServiceType.TB_CORE);
            for (TransportProtos.ServiceInfo service : tbCoreServices) {
                TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, service.getServiceId());
                ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setComponentLifecycleMsg(ByteString.copyFrom(msgBytes)).build();
                toCoreNfProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toCoreMsg), null);
                toCoreNfs.incrementAndGet();
            }
            // No need to push notifications twice
            tbRuleEngineServices.removeAll(tbCoreServices);
        }
        for (TransportProtos.ServiceInfo service : tbRuleEngineServices) {
            TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, service.getServiceId());
            ToRuleEngineNotificationMsg toRuleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setComponentLifecycleMsg(ByteString.copyFrom(msgBytes)).build();
            toRuleEngineProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toRuleEngineMsg), null);
            toRuleEngineNfs.incrementAndGet();
        }
    }

    @Scheduled(fixedDelayString = "${cluster.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            int toCoreMsgCnt = toCoreMsgs.getAndSet(0);
            int toCoreNfsCnt = toCoreNfs.getAndSet(0);
            int toRuleEngineMsgsCnt = toRuleEngineMsgs.getAndSet(0);
            int toRuleEngineNfsCnt = toRuleEngineNfs.getAndSet(0);
            int toTransportNfsCnt = toTransportNfs.getAndSet(0);
            if (toCoreMsgCnt > 0 || toCoreNfsCnt > 0 || toRuleEngineMsgsCnt > 0 || toRuleEngineNfsCnt > 0 || toTransportNfsCnt > 0) {
                log.info("To TbCore: [{}] messages [{}] notifications; To TbRuleEngine: [{}] messages [{}] notifications; To Transport: [{}] notifications",
                        toCoreMsgCnt, toCoreNfsCnt, toRuleEngineMsgsCnt, toRuleEngineNfsCnt, toTransportNfsCnt);
            }
        }
    }
}
