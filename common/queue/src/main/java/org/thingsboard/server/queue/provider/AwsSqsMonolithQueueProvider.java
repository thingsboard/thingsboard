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
package org.thingsboard.server.queue.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueCoreSettings;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsConsumerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsProducerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsSettings;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && '${service.type:null}'=='monolith'")
public class AwsSqsMonolithQueueProvider implements TbCoreQueueProvider, TbRuleEngineQueueProvider {
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    private final TbAwsSqsSettings sqsSettings;
    private final TbQueueAdmin admin;
    private TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> tbCoreProducer;

    public AwsSqsMonolithQueueProvider(TbQueueCoreSettings coreSettings,
                                       TbQueueRuleEngineSettings ruleEngineSettings,
                                       TbQueueTransportApiSettings transportApiSettings,
                                       TbQueueTransportNotificationSettings transportNotificationSettings,
                                       TbAwsSqsSettings sqsSettings) {
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.sqsSettings = sqsSettings;
        admin = new TbAwsSqsAdmin(sqsSettings);
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToTransportMsg>> getTransportNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(admin, sqsSettings, transportNotificationSettings.getNotificationsTopic());
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(admin, sqsSettings, ruleEngineSettings.getTopic());
    }

    //TODO 2.5 Singleton
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getTbCoreMsgProducer() {
        if (tbCoreProducer == null) {
            synchronized (this) {
                if (tbCoreProducer == null) {
                    tbCoreProducer = new TbAwsSqsProducerTemplate<>(admin, sqsSettings, coreSettings.getTopic());
                }
            }
        }
        return tbCoreProducer;
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getToRuleEngineMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(admin, sqsSettings, ruleEngineSettings.getTopic(), msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> getToCoreMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(admin, sqsSettings, coreSettings.getTopic(), msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> getTransportApiRequestConsumer() {
        return new TbAwsSqsConsumerTemplate<>(admin, sqsSettings, transportApiSettings.getRequestsTopic(), msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportProtos.TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> getTransportApiResponseProducer() {
        return new TbAwsSqsProducerTemplate<>(admin, sqsSettings, transportApiSettings.getResponsesTopic());
    }
}
