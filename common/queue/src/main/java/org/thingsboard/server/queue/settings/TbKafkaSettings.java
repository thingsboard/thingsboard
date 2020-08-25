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
package org.thingsboard.server.queue.settings;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.kafka.TbKafkaProperty;

import java.util.List;
import java.util.Properties;

/**
 * Created by ashvayka on 25.09.18.
 */
@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='kafka'")
@ConfigurationProperties(prefix = "queue.kafka")
@Component
public class TbKafkaSettings {

    @Value("${queue.kafka.bootstrap.servers}")
    private String servers;

    @Value("${queue.kafka.acks}")
    private String acks;

    @Value("${queue.kafka.retries}")
    private int retries;

    @Value("${queue.kafka.batch.size}")
    private int batchSize;

    @Value("${queue.kafka.linger.ms}")
    private long lingerMs;

    @Value("${queue.kafka.buffer.memory}")
    private long bufferMemory;

    @Value("${queue.kafka.replication_factor}")
    @Getter
    private short replicationFactor;

    @Value("${queue.kafka.max_poll_records:8192}")
    @Getter
    private int maxPollRecords;

    @Value("${queue.kafka.max_partition_fetch_bytes:16777216}")
    @Getter
    private int maxPartitionFetchBytes;

    @Value("${queue.kafka.fetch_max_bytes:134217728}")
    @Getter
    private int fetchMaxBytes;

    @Value("${queue.kafka.use_confluent_cloud:false}")
    private boolean useConfluent;

    @Value("${queue.kafka.confluent.ssl.algorithm}")
    private String sslAlgorithm;

    @Value("${queue.kafka.confluent.sasl.mechanism}")
    private String saslMechanism;

    @Value("${queue.kafka.confluent.sasl.config}")
    private String saslConfig;

    @Value("${queue.kafka.confluent.security.protocol}")
    private String securityProtocol;

    @Setter
    private List<TbKafkaProperty> other;

    public Properties toProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);

        if (useConfluent) {
            props.put("ssl.endpoint.identification.algorithm", sslAlgorithm);
            props.put("sasl.mechanism", saslMechanism);
            props.put("sasl.jaas.config", saslConfig);
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        } else {
            props.put(ProducerConfig.ACKS_CONFIG, acks);
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
            props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        }

        if (other != null) {
            other.forEach(kv -> props.put(kv.getKey(), kv.getValue()));
        }
        return props;
    }
}
