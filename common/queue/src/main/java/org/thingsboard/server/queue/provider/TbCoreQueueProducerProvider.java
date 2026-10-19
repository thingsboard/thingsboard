// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.queue.provider;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorDownlinkMsg;
import org.thingsboard.server.gen.integration.ToIntegrationExecutorNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToAgentNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeEventNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdgeNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTbReportNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
public class TbCoreQueueProducerProvider implements TbQueueProducerProvider {

    private static final String NOT_IMPLEMENTED = "Not Implemented! Should not be used by TB Core!";
    private final TbCoreQueueFactory tbQueueProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> toTransport;
    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> toRuleEngine;
    private TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> toTbCore;
    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> toRuleEngineNotifications;
    private TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toTbCoreNotifications;
    private TbQueueProducer<TbProtoQueueMsg<ToEdgeMsg>> toEdge;
    private TbQueueProducer<TbProtoQueueMsg<ToEdgeNotificationMsg>> toEdgeNotifications;
    private TbQueueProducer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> toEdgeEvents;
    private TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> toIntegrationExecutorNotifications;
    private TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> toUsageStats;
    private TbQueueProducer<TbProtoQueueMsg<ToVersionControlServiceMsg>> toVersionControl;
    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> toIntegrationRuleEngine;
    private TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> toIntegrationDownlink;
    private TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> toHousekeeper;
    private TbQueueProducer<TbProtoQueueMsg<ToCalculatedFieldMsg>> toCalculatedFields;
    private TbQueueProducer<TbProtoQueueMsg<ToCalculatedFieldNotificationMsg>> toCalculatedFieldNotifications;
    private TbQueueProducer<TbProtoQueueMsg<ToTbReportNotificationMsg>> toTbReportNotifications;
    private TbQueueProducer<TbProtoQueueMsg<ToAgentNotificationMsg>> toAgentNotifications;
    private TbQueueProducer<TbProtoQueueMsg<AgentBulkOperationMsg>> toAgentBulkOps;

    public TbCoreQueueProducerProvider(TbCoreQueueFactory tbQueueProvider) {
        this.tbQueueProvider = tbQueueProvider;
    }

    @PostConstruct
    public void init() {
        this.toTbCore = tbQueueProvider.createTbCoreMsgProducer();
        this.toTransport = tbQueueProvider.createTransportNotificationsMsgProducer();
        this.toRuleEngine = tbQueueProvider.createRuleEngineMsgProducer();
        this.toRuleEngineNotifications = tbQueueProvider.createRuleEngineNotificationsMsgProducer();
        this.toTbCoreNotifications = tbQueueProvider.createTbCoreNotificationsMsgProducer();
        this.toIntegrationExecutorNotifications = tbQueueProvider.createIntegrationExecutorNotificationsMsgProducer();
        this.toIntegrationDownlink = tbQueueProvider.createIntegrationExecutorDownlinkMsgProducer();
        this.toUsageStats = tbQueueProvider.createToUsageStatsServiceMsgProducer();
        this.toVersionControl = tbQueueProvider.createVersionControlMsgProducer();
        this.toIntegrationRuleEngine = tbQueueProvider.createIntegrationRuleEngineMsgProducer();
        this.toHousekeeper = tbQueueProvider.createHousekeeperMsgProducer();
        this.toEdge = tbQueueProvider.createEdgeMsgProducer();
        this.toEdgeNotifications = tbQueueProvider.createEdgeNotificationsMsgProducer();
        this.toEdgeEvents = tbQueueProvider.createEdgeEventMsgProducer();
        this.toCalculatedFields = tbQueueProvider.createToCalculatedFieldMsgProducer();
        this.toCalculatedFieldNotifications = tbQueueProvider.createToCalculatedFieldNotificationMsgProducer();
        this.toTbReportNotifications = tbQueueProvider.createTbReportNotificationsMsgProducer();
        this.toAgentNotifications = tbQueueProvider.createAgentNotificationsMsgProducer();
        this.toAgentBulkOps = tbQueueProvider.createAgentBulkOpsMsgProducer();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> getTransportNotificationsMsgProducer() {
        return toTransport;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return toRuleEngine;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> getRuleEngineNotificationsMsgProducer() {
        return toRuleEngineNotifications;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer() {
        return toTbCore;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreIntegrationMsg>> getTbCoreIntegrationMsgProducer() {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> getTbCoreNotificationsMsgProducer() {
        return toTbCoreNotifications;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorNotificationMsg>> getTbIntegrationExecutorNotificationsMsgProducer() {
        return toIntegrationExecutorNotifications;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> getTbUsageStatsMsgProducer() {
        return toUsageStats;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToVersionControlServiceMsg>> getTbVersionControlMsgProducer() {
        return toVersionControl;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> getHousekeeperMsgProducer() {
        return toHousekeeper;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdgeMsg>> getTbEdgeMsgProducer() {
        return toEdge;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdgeNotificationMsg>> getTbEdgeNotificationsMsgProducer() {
        return toEdgeNotifications;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToEdgeEventNotificationMsg>> getTbEdgeEventsMsgProducer() {
        return toEdgeEvents;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getIntegrationRuleEngineMsgProducer() {
        return toIntegrationRuleEngine;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToIntegrationExecutorDownlinkMsg>> getTbIntegrationExecutorDownlinkMsgProducer() {
        return toIntegrationDownlink;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToAgentNotificationMsg>> getTbAgentNotificationsMsgProducer() {
        return toAgentNotifications;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<AgentBulkOperationMsg>> getTbAgentBulkOpsMsgProducer() {
        return toAgentBulkOps;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCalculatedFieldMsg>> getCalculatedFieldsMsgProducer() {
        return toCalculatedFields;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCalculatedFieldNotificationMsg>> getCalculatedFieldsNotificationsMsgProducer() {
        return toCalculatedFieldNotifications;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTbReportNotificationMsg>> getTbReportNotificationsMsgProducer() {
        return toTbReportNotifications;
    }

}
