/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueCoreSettings;
import org.thingsboard.server.TbQueueProducer;
import org.thingsboard.server.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.kafka.TBKafkaConsumerTemplate;
import org.thingsboard.server.kafka.TBKafkaProducerTemplate;
import org.thingsboard.server.kafka.TbKafkaSettings;
import org.thingsboard.server.kafka.TbNodeIdProvider;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && '${service.type:null}'=='tb-rule-engine'")
public class KafkaTbRuleEngineQueueProvider implements TbRuleEngineQueueProvider {

    private final TbKafkaSettings kafkaSettings;
    private final TbNodeIdProvider nodeIdProvider;
    private final TbQueueCoreSettings coreSettings;

    public KafkaTbRuleEngineQueueProvider(TbKafkaSettings kafkaSettings, TbNodeIdProvider nodeIdProvider, TbQueueCoreSettings coreSettings) {
        this.kafkaSettings = kafkaSettings;
        this.nodeIdProvider = nodeIdProvider;
        this.coreSettings = coreSettings;
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> getTransportMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-transport-" + nodeIdProvider.getNodeId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-rule-engine-" + nodeIdProvider.getNodeId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer() {
        TBKafkaProducerTemplate.TBKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TBKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("producer-core-" + nodeIdProvider.getNodeId());
        requestBuilder.defaultTopic(coreSettings.getTopic());
        return requestBuilder.build();
    }

    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> getToRuleEngineMsgConsumer() {
        TBKafkaConsumerTemplate.TBKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> responseBuilder = TBKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(coreSettings.getTopic());
        responseBuilder.clientId("tb-rule-engine-consumer-" + nodeIdProvider.getNodeId());
        responseBuilder.groupId("tb-rule-engine-" + nodeIdProvider.getNodeId());
        responseBuilder.autoCommit(true);
        responseBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToRuleEngineMsg.parseFrom(msg.getData()), msg.getHeaders()));
        return responseBuilder.build();
    }
}
