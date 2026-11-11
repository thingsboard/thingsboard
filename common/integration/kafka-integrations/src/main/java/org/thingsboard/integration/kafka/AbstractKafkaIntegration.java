// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.integration.api.AbstractIntegration;
import org.thingsboard.integration.api.IntegrationContext;
import org.thingsboard.integration.api.TbIntegrationInitParams;
import org.thingsboard.server.common.data.exception.ThingsboardKafkaClientError;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractKafkaIntegration<T extends KafkaIntegrationMsg> extends AbstractIntegration<T> {

    protected KafkaConsumerConfiguration kafkaConsumerConfiguration;
    protected Consumer<String, String> kafkaConsumer;
    protected IntegrationContext ctx;
    protected ExecutorService loopExecutor;
    protected volatile boolean stopped = false;
    protected long pollInterval;
    protected Lock kafkaLock = new ReentrantLock();

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
        loopExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-loop"));
        this.ctx = params.getContext();
        kafkaConsumerConfiguration = getClientConfiguration(configuration, KafkaConsumerConfiguration.class);
    }

    @Override
    public void process(T msg) {
        String status = "OK";
        Exception exception = null;
        try {
            doProcess(context, msg);
            integrationStatistics.incMessagesProcessed();
        } catch (Exception e) {
            log.debug("Failed to apply data converter function: {}", e.getMessage(), e);
            exception = e;
            status = "ERROR";
        }
        if (!status.equals("OK")) {
            integrationStatistics.incErrorsOccurred();
        }
        persistDebug(context, "Uplink", getDefaultUplinkContentType(), () -> JacksonUtil.toString(msg.getMsg()), status, exception);
    }

    @Override
    public void destroy() {
        stopped = true;
        if (loopExecutor != null) {
            loopExecutor.shutdownNow();
        }
        if (kafkaConsumer != null) {
            kafkaLock.lock();
            try {
                kafkaConsumer.unsubscribe();
                kafkaConsumer.close();
            } finally {
                kafkaLock.unlock();
            }

        }
    }

    protected void initConsumer(KafkaConsumerConfiguration configuration) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, configuration.getClientId());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, configuration.getGroupId());
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.getBootstrapServers());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, configuration.getAutoCreateTopics());
        if (configuration.getOtherProperties() != null) {
            configuration.getOtherProperties().forEach(properties::put);
        }
        kafkaConsumer = createConsumer(properties);

        List<String> topics = parseTopics(configuration.getTopics());
        if (topics.isEmpty()) {
            throw new RuntimeException("No topics specified for the Kafka integration.");
        }

        kafkaLock.lock();
        try {
            kafkaConsumer.subscribe(topics);
            topics.forEach(kafkaConsumer::partitionsFor);
        } catch (Exception | ThingsboardKafkaClientError e) {
            throw new RuntimeException("Connection to node could not be established. Broker may not be available.", e);
        } finally {
            kafkaLock.unlock();
        }

        pollInterval = configuration.getPollInterval();
        stopped = false;
    }

    protected Consumer<String, String> createConsumer(Properties properties) {
        return new KafkaConsumer<>(properties);
    }

    static List<String> parseTopics(String topics) {
        if (topics == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(topics.split(","))
                .map(String::trim)
                .filter(topic -> !topic.isEmpty())
                .collect(Collectors.toList());
    }

    protected abstract void doProcess(IntegrationContext context, T msg) throws Exception;

}
