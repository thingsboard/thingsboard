/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.queue.kafka;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TbProperty;
import org.thingsboard.server.queue.util.PropertyUtils;
import org.thingsboard.server.queue.util.TbKafkaComponent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by ashvayka on 25.09.18.
 */
@Slf4j
@TbKafkaComponent
@ConfigurationProperties(prefix = "queue.kafka")
@Component
public class TbKafkaSettings {

    @Value("${queue.kafka.bootstrap.servers}")
    private String servers;

    @Value("${queue.kafka.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${queue.kafka.ssl.truststore.location:}")
    private String sslTruststoreLocation;

    @Value("${queue.kafka.ssl.truststore.password:}")
    private String sslTruststorePassword;

    @Value("${queue.kafka.ssl.keystore.location:}")
    private String sslKeystoreLocation;

    @Value("${queue.kafka.ssl.keystore.password:}")
    private String sslKeystorePassword;

    @Value("${queue.kafka.ssl.key.password:}")
    private String sslKeyPassword;

    @Value("${queue.kafka.acks:all}")
    private String acks;

    @Value("${queue.kafka.retries:1}")
    private int retries;

    @Value("${queue.kafka.compression.type:none}")
    private String compressionType;

    @Value("${queue.kafka.batch.size:16384}")
    private int batchSize;

    @Value("${queue.kafka.linger.ms:1}")
    private long lingerMs;

    @Value("${queue.kafka.max.request.size:1048576}")
    private int maxRequestSize;

    @Value("${queue.kafka.max.in.flight.requests.per.connection:5}")
    private int maxInFlightRequestsPerConnection;

    @Value("${queue.kafka.buffer.memory:33554432}")
    private long bufferMemory;

    @Value("${queue.kafka.replication_factor:1}")
    @Getter
    private short replicationFactor;

    @Value("${queue.kafka.max_poll_records:8192}")
    private int maxPollRecords;

    @Value("${queue.kafka.max_poll_interval_ms:300000}")
    private int maxPollIntervalMs;

    @Value("${queue.kafka.max_partition_fetch_bytes:16777216}")
    private int maxPartitionFetchBytes;

    @Value("${queue.kafka.fetch_max_bytes:134217728}")
    private int fetchMaxBytes;

    @Getter
    @Value("${queue.kafka.request.timeout.ms:30000}")
    private int requestTimeoutMs;

    @Value("${queue.kafka.session.timeout.ms:10000}")
    private int sessionTimeoutMs;

    @Value("${queue.kafka.auto_offset_reset:earliest}")
    private String autoOffsetReset;

    @Value("${queue.kafka.use_confluent_cloud:false}")
    private boolean useConfluent;

    @Value("${queue.kafka.confluent.ssl.algorithm:}")
    private String sslAlgorithm;

    @Value("${queue.kafka.confluent.sasl.mechanism:}")
    private String saslMechanism;

    @Value("${queue.kafka.confluent.sasl.config:}")
    private String saslConfig;

    @Value("${queue.kafka.confluent.security.protocol:}")
    private String securityProtocol;

    @Value("${queue.kafka.other-inline:}")
    private String otherInline;

    @Value("${queue.kafka.consumer-properties-per-topic-inline:}")
    private String consumerPropertiesPerTopicInline;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Deprecated
    @Setter
    private List<TbProperty> other;

    @Setter
    private Map<String, List<TbProperty>> consumerPropertiesPerTopic = new HashMap<>();

    @PostConstruct
    public void initInlineTopicProperties() {
        Map<String, List<TbProperty>> inlineProps = parseTopicPropertyList(consumerPropertiesPerTopicInline);
        if (!inlineProps.isEmpty()) {
            consumerPropertiesPerTopic.putAll(inlineProps);
        }
    }

    public Properties toConsumerProps(String topic) {
        Properties props = toProps();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes);
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetchMaxBytes);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        if (topic != null) {
            List<TbProperty> properties = consumerPropertiesPerTopic.get(topic);
            if (properties == null) {
                for (Map.Entry<String, List<TbProperty>> entry : consumerPropertiesPerTopic.entrySet()) {
                    if (topic.startsWith(entry.getKey())) {
                        properties = entry.getValue();
                        break;
                    }
                }
            }
            if (properties != null) {
                properties.forEach(kv -> props.put(kv.getKey(), kv.getValue()));
            }
        }
        return props;
    }

    public Properties toProducerProps() {
        Properties props = toProps();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequestsPerConnection);
        return props;
    }

    Properties toProps() {
        Properties props = new Properties();

        if (useConfluent) {
            props.put("ssl.endpoint.identification.algorithm", sslAlgorithm);
            props.put("sasl.mechanism", saslMechanism);
            props.put("sasl.jaas.config", saslConfig);
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        }

        props.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        props.putAll(PropertyUtils.getProps(otherInline));

        if (other != null) {
            other.forEach(kv -> props.put(kv.getKey(), kv.getValue()));
        }

        configureSSL(props);

        return props;
    }

    void configureSSL(Properties props) {
        if (sslEnabled) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslTruststoreLocation);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, sslTruststorePassword);
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, sslKeystoreLocation);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, sslKeystorePassword);
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
        }
    }

    /*
     * Temporary solution to avoid major code changes.
     * FIXME: use single instance of Kafka queue admin, don't create a separate one for each consumer/producer
     * */
    public KafkaAdmin getAdmin() {
        return kafkaAdmin;
    }

    protected Properties toAdminProps() {
        Properties props = toProps();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(AdminClientConfig.RETRIES_CONFIG, retries);
        return props;
    }

    private Map<String, List<TbProperty>> parseTopicPropertyList(String inlineProperties) {
        Map<String, List<String>> grouped = PropertyUtils.getGroupedProps(inlineProperties);
        Map<String, List<TbProperty>> result = new HashMap<>();

        grouped.forEach((topic, entries) -> {
            Map<String, String> merged = new LinkedHashMap<>();
            for (String entry : entries) {
                String[] kv = entry.split("=", 2);
                if (kv.length == 2) {
                    merged.put(kv[0].trim(), kv[1].trim());
                }
            }
            List<TbProperty> props = merged.entrySet().stream()
                    .map(e -> new TbProperty(e.getKey(), e.getValue()))
                    .toList();
            result.put(topic, props);
        });

        return result;
    }

}
