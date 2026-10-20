// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.provider;

import org.thingsboard.server.gen.integration.IntegrationApiRequestMsg;
import org.thingsboard.server.gen.integration.IntegrationApiResponseMsg;
import org.thingsboard.server.gen.integration.ToCoreIntegrationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

/**
 * Responsible for initialization of various Producers and Consumers used by TB Integration Executor Node.
 * Implementation Depends on the queue queue.type from yml or TB_QUEUE_TYPE environment variable
 */
public interface TbIntegrationExecutorQueueFactory extends TbCoreIntegrationExecutorQueueFactory, HousekeeperClientQueueFactory {

    TbQueueProducer<TbProtoQueueMsg<ToCoreIntegrationMsg>> createTbCoreIntegrationMsgProducer();

    TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationMsgProducer();

    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer();

    TbQueueRequestTemplate<TbProtoQueueMsg<IntegrationApiRequestMsg>, TbProtoQueueMsg<IntegrationApiResponseMsg>> createIntegrationApiRequestTemplate();

}
