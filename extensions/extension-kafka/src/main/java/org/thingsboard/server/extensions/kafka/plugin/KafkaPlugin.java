/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.kafka.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.kafka.action.KafkaPluginAction;

import java.util.Properties;

@Plugin(name = "Kafka Plugin", actions = {KafkaPluginAction.class},
        descriptor = "KafkaPluginDescriptor.json", configuration = KafkaPluginConfiguration.class)
@Slf4j
public class KafkaPlugin extends AbstractPlugin<KafkaPluginConfiguration> {

    private KafkaMsgHandler handler;
    private Producer<?, String> producer;
    private final Properties properties = new Properties();

    @Override
    public void init(KafkaPluginConfiguration configuration) {
        properties.put("bootstrap.servers", configuration.getBootstrapServers());
        properties.put("value.serializer", configuration.getValueSerializer());
        properties.put("key.serializer", configuration.getKeySerializer());
        properties.put("acks", String.valueOf(configuration.getAcks()));
        properties.put("retries", configuration.getRetries());
        properties.put("batch.size", configuration.getBatchSize());
        properties.put("linger.ms", configuration.getLinger());
        properties.put("buffer.memory", configuration.getBufferMemory());
        if (configuration.getOtherProperties() != null) {
            configuration.getOtherProperties()
                    .forEach(p -> properties.put(p.getKey(), p.getValue()));
        }
        init();
    }

    private void init() {
        try {
            this.producer = new KafkaProducer<>(properties);
            this.handler = new KafkaMsgHandler(producer);
        } catch (Exception e) {
            log.error("Failed to start kafka producer", e);
            throw new RuntimeException(e);
        }
    }

    private void destroy() {
        try {
            this.handler = null;
            this.producer.close();
        } catch (Exception e) {
            log.error("Failed to close producer during destroy()", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return handler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
    }

    @Override
    public void suspend(PluginContext ctx) {
        destroy();
    }

    @Override
    public void stop(PluginContext ctx) {
        destroy();
    }
}
