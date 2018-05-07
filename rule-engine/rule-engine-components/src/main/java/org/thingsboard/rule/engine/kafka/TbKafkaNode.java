/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "kafka",
        configClazz = TbKafkaNodeConfiguration.class,
        nodeDescription = "Publish messages to Kafka server",
        nodeDetails = "Expects messages with any message type. Will send record via Kafka producer to Kafka server.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeKafkaConfig"
)
public class TbKafkaNode implements TbNode {

    private static final String OFFSET = "offset";
    private static final String PARTITION = "partition";
    private static final String TOPIC = "topic";
    private static final String ERROR = "error";

    private TbKafkaNodeConfiguration config;

    private Producer<?, String> producer;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbKafkaNodeConfiguration.class);
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.getValueSerializer());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.getKeySerializer());
        properties.put(ProducerConfig.ACKS_CONFIG, config.getAcks());
        properties.put(ProducerConfig.RETRIES_CONFIG, config.getRetries());
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, config.getBatchSize());
        properties.put(ProducerConfig.LINGER_MS_CONFIG, config.getLinger());
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, config.getBufferMemory());
        if (config.getOtherProperties() != null) {
            config.getOtherProperties()
                    .forEach((k,v) -> properties.put(k, v));
        }
        try {
            this.producer = new KafkaProducer<>(properties);
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        String topic = TbNodeUtils.processPattern(config.getTopicPattern(), msg.getMetaData());
        try {
            producer.send(new ProducerRecord<>(topic, msg.getData()),
                    (metadata, e) -> {
                        if (metadata != null) {
                            TbMsg next = processResponse(ctx, msg, metadata);
                            ctx.tellNext(next, TbRelationTypes.SUCCESS);
                        } else {
                            TbMsg next = processException(ctx, msg, e);
                            ctx.tellNext(next, TbRelationTypes.FAILURE, e);
                        }
                    });
        } catch (Exception e) {
            ctx.tellError(msg, e);
        }
    }

    @Override
    public void destroy() {
        if (this.producer != null) {
            try {
                this.producer.close();
            } catch (Exception e) {
                log.error("Failed to close producer during destroy()", e);
            }
        }
    }

    private TbMsg processResponse(TbContext ctx, TbMsg origMsg, RecordMetadata recordMetadata) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(OFFSET, String.valueOf(recordMetadata.offset()));
        metaData.putValue(PARTITION, String.valueOf(recordMetadata.partition()));
        metaData.putValue(TOPIC, recordMetadata.topic());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Exception e) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

}
