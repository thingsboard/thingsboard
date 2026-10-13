// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.junit.jupiter.api.Test;
import org.thingsboard.integration.api.IntegrationContext;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractKafkaIntegrationTest {

    @Test
    void parseTopicsSplitsCommaSeparatedList() {
        assertThat(AbstractKafkaIntegration.parseTopics("topica,topicb")).containsExactly("topica", "topicb");
    }

    @Test
    void parseTopicsTrimsWhitespaceAroundEachTopic() {
        assertThat(AbstractKafkaIntegration.parseTopics(" topica , topicb ")).containsExactly("topica", "topicb");
    }

    @Test
    void parseTopicsReturnsSingleTopic() {
        assertThat(AbstractKafkaIntegration.parseTopics("topica")).containsExactly("topica");
    }

    @Test
    void parseTopicsIgnoresEmptyEntries() {
        assertThat(AbstractKafkaIntegration.parseTopics("topica,,topicb,")).containsExactly("topica", "topicb");
    }

    @Test
    void parseTopicsReturnsEmptyListForNull() {
        assertThat(AbstractKafkaIntegration.parseTopics(null)).isEmpty();
    }

    @Test
    void parseTopicsReturnsEmptyListForBlank() {
        assertThat(AbstractKafkaIntegration.parseTopics("   ")).isEmpty();
    }

    @Test
    void initConsumerSubscribesToAllTopicsFromCommaSeparatedList() {
        MockConsumer<String, String> mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TestKafkaIntegration integration = new TestKafkaIntegration(mockConsumer);

        integration.initConsumer(configuration("topica, topicb"));

        assertThat(mockConsumer.subscription()).containsExactlyInAnyOrder("topica", "topicb");
    }

    @Test
    void initConsumerSubscribesToSingleTopic() {
        MockConsumer<String, String> mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TestKafkaIntegration integration = new TestKafkaIntegration(mockConsumer);

        integration.initConsumer(configuration("topica"));

        assertThat(mockConsumer.subscription()).containsExactly("topica");
    }

    @Test
    void initConsumerFailsWhenNoTopicsConfigured() {
        MockConsumer<String, String> mockConsumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TestKafkaIntegration integration = new TestKafkaIntegration(mockConsumer);

        assertThatThrownBy(() -> integration.initConsumer(configuration("")))
                .isInstanceOf(RuntimeException.class);
    }

    private static KafkaConsumerConfiguration configuration(String topics) {
        KafkaConsumerConfiguration configuration = new KafkaConsumerConfiguration();
        configuration.setClientId("test-client");
        configuration.setGroupId("test-group");
        configuration.setBootstrapServers("localhost:9092");
        configuration.setAutoCreateTopics("false");
        configuration.setTopics(topics);
        configuration.setPollInterval(100);
        return configuration;
    }

    private static class TestKafkaIntegration extends AbstractKafkaIntegration<KafkaIntegrationMsg> {

        private final Consumer<String, String> consumer;

        TestKafkaIntegration(Consumer<String, String> consumer) {
            this.consumer = consumer;
        }

        @Override
        protected Consumer<String, String> createConsumer(Properties properties) {
            return consumer;
        }

        @Override
        protected void doProcess(IntegrationContext context, KafkaIntegrationMsg msg) {
        }
    }
}
