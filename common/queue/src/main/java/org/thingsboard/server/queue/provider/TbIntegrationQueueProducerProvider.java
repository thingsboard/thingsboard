// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.provider;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.util.TbIntegrationExecutorComponent;

@Service
@TbIntegrationExecutorComponent
public class TbIntegrationQueueProducerProvider implements TbQueueProducerProvider {

    private static final String NOT_IMPLEMENTED = "Not Implemented! Should not be used by Integration Executor!";

    private final TbIntegrationExecutorQueueFactory tbQueueProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToCoreIntegrationMsg>> toTbCore;
    private TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toTbCoreNf;
    private TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> toUsageStats;
    private TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> toHousekeeper;
    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> toRuleEngine;

    public TbIntegrationQueueProducerProvider(TbIntegrationExecutorQueueFactory tbQueueProvider) {
        this.tbQueueProvider = tbQueueProvider;
    }

    @PostConstruct
    public void init() {
        this.toTbCore = tbQueueProvider.createTbCoreIntegrationMsgProducer();
        this.toTbCoreNf = tbQueueProvider.createTbCoreNotificationMsgProducer();
        this.toUsageStats = tbQueueProvider.createToUsageStatsServiceMsgProducer();
        this.toHousekeeper = tbQueueProvider.createHousekeeperMsgProducer();
        this.toRuleEngine = tbQueueProvider.createRuleEngineMsgProducer();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> getTransportNotificationsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return toRuleEngine;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> getRuleEngineNotificationsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreIntegrationMsg>> getTbCoreIntegrationMsgProducer() {
        return toTbCore;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> getTbCoreNotificationsMsgProducer() {
        return toTbCoreNf;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> getTbUsageStatsMsgProducer() {
        return toUsageStats;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> getTbVersionControlMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getIntegrationRuleEngineMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToEdgeMsg>> getTbEdgeMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToEdgeNotificationMsg>> getTbEdgeNotificationsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToEdgeEventNotificationMsg>> getTbEdgeEventsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCalculatedFieldMsg>> getCalculatedFieldsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCalculatedFieldNotificationMsg>> getCalculatedFieldsNotificationsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> getTbIntegrationExecutorNotificationsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> getTbIntegrationExecutorDownlinkMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> getHousekeeperMsgProducer() {
        return toHousekeeper;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTbReportNotificationMsg>> getTbReportNotificationsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToAgentNotificationMsg>> getTbAgentNotificationsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.AgentBulkOperationMsg>> getTbAgentBulkOpsMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

}
