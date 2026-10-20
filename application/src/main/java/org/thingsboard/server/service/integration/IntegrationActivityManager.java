// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.transport.activity.AbstractActivityManager;
import org.thingsboard.server.common.transport.activity.ActivityReportCallback;
import org.thingsboard.server.common.transport.activity.ActivityState;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategy;
import org.thingsboard.server.common.transport.activity.strategy.ActivityStrategyType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;

@Slf4j
public abstract class IntegrationActivityManager extends AbstractActivityManager<IntegrationActivityKey, Void> {

    @Autowired
    protected PartitionService partitionService;

    @Autowired
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreMsgProducer;

    @Value("${integrations.activity.reporting_period:3000}")
    private long reportingPeriodMillis;

    @Value("${integrations.activity.reporting_strategy:LAST}")
    private ActivityStrategyType reportingStrategyType;

    @PostConstruct
    public void init() {
        super.init();
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
    }

    @Override
    protected long getReportingPeriodMillis() {
        return reportingPeriodMillis;
    }

    @Override
    protected ActivityStrategy getStrategy() {
        return reportingStrategyType.toStrategy();
    }

    @Override
    protected ActivityState<Void> updateState(IntegrationActivityKey key, ActivityState<Void> state) {
        return state;
    }

    @Override
    protected boolean hasExpired(long lastRecordedTime) {
        return (getCurrentTimeMillis() - reportingPeriodMillis) > lastRecordedTime;
    }

    @Override
    protected void onStateExpiry(IntegrationActivityKey key, Void currentMetadata) {
    }

    @Override
    protected void reportActivity(IntegrationActivityKey key, Void metadata, long timeToReport, ActivityReportCallback<IntegrationActivityKey> callback) {
        var tenantId = key.getTenantId();
        var deviceId = key.getDeviceId();
        log.debug("[{}][{}] Reporting activity state. Time to report: [{}].", tenantId.getId(), deviceId.getId(), timeToReport);
        TransportProtos.ToCoreMsg toCoreMsg = buildActivityMsg(tenantId, deviceId, timeToReport);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        tbCoreMsgProducer.send(tpi, new TbProtoQueueMsg<>(deviceId.getId(), toCoreMsg), new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata msgAcknowledged) {
                callback.onSuccess(key, timeToReport);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(key, t);
            }
        });
    }

    private TransportProtos.ToCoreMsg buildActivityMsg(TenantId tenantId, DeviceId deviceId, long lastActivityTime) {
        var tenantUuid = tenantId.getId();
        var deviceUuid = deviceId.getId();
        TransportProtos.DeviceActivityProto deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                .setTenantIdMSB(tenantUuid.getMostSignificantBits())
                .setTenantIdLSB(tenantUuid.getLeastSignificantBits())
                .setDeviceIdMSB(deviceUuid.getMostSignificantBits())
                .setDeviceIdLSB(deviceUuid.getLeastSignificantBits())
                .setLastActivityTime(lastActivityTime)
                .build();
        return TransportProtos.ToCoreMsg.newBuilder()
                .setDeviceActivityMsg(deviceActivityMsg)
                .build();
    }

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

}
