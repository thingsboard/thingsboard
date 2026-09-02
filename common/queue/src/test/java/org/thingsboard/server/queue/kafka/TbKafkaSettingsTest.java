/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

class TbKafkaSettingsTest {

    @Nested
    @SpringBootTest(classes = {TbKafkaSettings.class, KafkaAdmin.class})
    @TestPropertySource(properties = {
            "queue.type=kafka",
            "queue.kafka.bootstrap.servers=localhost:9092",
            "queue.kafka.other-inline=metrics.recording.level:INFO;metrics.sample.window.ms:30000",
            "queue.kafka.consumer-properties-per-topic-inline=" +
                    "tb_core_updated:max.poll.records=10;" +
                    "tb_core_updated:enable.auto.commit=true;" +
                    "tb_core_updated:bootstrap.servers=kafka1:9092,kafka2:9092;" +
                    "tb_edge_updated:max.poll.records=5;" +
                    "tb_edge_updated:auto.offset.reset=latest"
    })
    class InlinePropertiesAndSsl {

        @Autowired
        TbKafkaSettings settings;

        @BeforeEach
        void beforeEach() {
            settings = spy(settings); // SpyBean is not aware on @ConditionalOnProperty, that is why the traditional spy in use
        }

        @Test
        void givenToProps_whenConfigureSSL_thenVerifyOnce() {
            Properties props = settings.toProps();

            assertThat(props).as("TB_QUEUE_KAFKA_REQUEST_TIMEOUT_MS").containsEntry("request.timeout.ms", 30000);

            //other-inline
            assertThat(props).as("metrics.recording.level").containsEntry("metrics.recording.level", "INFO");
            assertThat(props).as("TB_QUEUE_KAFKA_SESSION_TIMEOUT_MS").containsEntry("metrics.sample.window.ms", "30000");

            Mockito.verify(settings).toProps();
            Mockito.verify(settings).configureSSL(any());
        }

        @Test
        void givenToAdminProps_whenConfigureSSL_thenVerifyOnce() {
            settings.toAdminProps();
            Mockito.verify(settings).toProps();
            Mockito.verify(settings).configureSSL(any());
        }

        @Test
        void givenToConsumerProps_whenConfigureSSL_thenVerifyOnce() {
            settings.toConsumerProps("main");
            Mockito.verify(settings).toProps();
            Mockito.verify(settings).configureSSL(any());
        }

        @Test
        void givenTotoProducerProps_whenConfigureSSL_thenVerifyOnce() {
            settings.toProducerProps();
            Mockito.verify(settings).toProps();
            Mockito.verify(settings).configureSSL(any());
        }

        @Test
        void givenMultipleTopicsInInlineConfig_whenParsed_thenEachTopicGetsExpectedProperties() {
            Properties coreProps = settings.toConsumerProps("tb_core_updated");
            assertThat(coreProps.getProperty("max.poll.records")).isEqualTo("10");
            assertThat(coreProps.getProperty("enable.auto.commit")).isEqualTo("true");
            assertThat(coreProps.getProperty("bootstrap.servers")).isEqualTo("kafka1:9092,kafka2:9092");

            Properties edgeProps = settings.toConsumerProps("tb_edge_updated");
            assertThat(edgeProps.getProperty("max.poll.records")).isEqualTo("5");
            assertThat(edgeProps.getProperty("auto.offset.reset")).isEqualTo("latest");
        }

    }

    @Nested
    @SpringBootTest(classes = {TbKafkaSettings.class, KafkaAdmin.class})
    @TestPropertySource(properties = {
            "queue.type=kafka",
            "queue.kafka.bootstrap.servers=localhost:9092",
            "queue.kafka.use_confluent_cloud=true",
            "queue.kafka.confluent.sasl.mechanism=OAUTHBEARER",
            "queue.kafka.confluent.security.protocol=SASL_SSL",
            "queue.kafka.confluent.oauth.client-id=my-client",
            "queue.kafka.confluent.oauth.client-secret=my-secret",
            "queue.kafka.confluent.oauth.endpoint-url=https://idp.example.com/oauth/token"
    })
    class ConfluentOAuth {

        @Autowired
        TbKafkaSettings settings;

        @Test
        void givenOauthbearerMechanism_whenToProps_thenConfiguresNativeOidcHandler() {
            Properties props = settings.toProps();

            assertThat(props).containsEntry("sasl.mechanism", "OAUTHBEARER");
            assertThat(props).containsEntry("security.protocol", "SASL_SSL");
            assertThat(props).containsEntry("sasl.login.callback.handler.class",
                    "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler");
            assertThat(props).containsEntry("sasl.oauthbearer.token.endpoint.url",
                    "https://idp.example.com/oauth/token");
            assertThat(props.getProperty("sasl.jaas.config"))
                    .contains("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required")
                    .contains("clientId=\"my-client\"")
                    .contains("clientSecret=\"my-secret\"");
        }

    }

    @Nested
    @SpringBootTest(classes = {TbKafkaSettings.class, KafkaAdmin.class})
    @TestPropertySource(properties = {
            "queue.type=kafka",
            "queue.kafka.bootstrap.servers=localhost:9092",
            "queue.kafka.use_confluent_cloud=true",
            "queue.kafka.confluent.sasl.mechanism=OAUTHBEARER",
            "queue.kafka.confluent.security.protocol=SASL_SSL",
            "queue.kafka.confluent.oauth.client-id=client\"with\\\\quote",
            "queue.kafka.confluent.oauth.client-secret=secret\"with\\\\backslash;and-semi",
            "queue.kafka.confluent.oauth.endpoint-url=https://idp.example.com/oauth/token"
    })
    class ConfluentOAuthEscaping {

        @Autowired
        TbKafkaSettings settings;

        // Locks in that double quotes and backslashes in client credentials are escaped
        // so the JAAS config string parses correctly and cannot inject extra options.
        @Test
        void givenSecretWithQuoteAndBackslash_whenToProps_thenEscapesJaasValues() {
            Properties props = settings.toProps();

            String jaas = props.getProperty("sasl.jaas.config");
            assertThat(jaas)
                    .contains("clientId=\"client\\\"with\\\\quote\"")
                    .contains("clientSecret=\"secret\\\"with\\\\backslash;and-semi\"")
                    .endsWith("\";");
        }

    }

    @Nested
    @SpringBootTest(classes = {TbKafkaSettings.class, KafkaAdmin.class})
    @TestPropertySource(properties = {
            "queue.type=kafka",
            "queue.kafka.bootstrap.servers=localhost:9092",
            "queue.kafka.use_confluent_cloud=true",
            "queue.kafka.confluent.sasl.mechanism=OAUTHBEARER",
            "queue.kafka.confluent.security.protocol=SASL_PLAINTEXT",
            "queue.kafka.confluent.oauth.client-id=my-client",
            "queue.kafka.confluent.oauth.client-secret=my-secret",
            "queue.kafka.confluent.oauth.endpoint-url=http://idp.example.com/oauth/token"
    })
    class ConfluentOAuthHttpWarning {

        @Autowired
        TbKafkaSettings settings;

        @Test
        void givenHttpEndpointUrl_whenToProps_thenProducesValidJaasConfigAndLogsWarning() {
            Logger logger = (Logger) LoggerFactory.getLogger(TbKafkaSettings.class);
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start();
            logger.addAppender(appender);
            try {
                Properties props = settings.toProps();

                assertThat(props.getProperty("sasl.jaas.config"))
                        .contains("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required")
                        .contains("clientId=\"my-client\"")
                        .contains("clientSecret=\"my-secret\"");
                assertThat(props).containsEntry("sasl.oauthbearer.token.endpoint.url",
                        "http://idp.example.com/oauth/token");

                assertThat(appender.list)
                        .filteredOn(e -> e.getLevel() == Level.WARN)
                        .anySatisfy(e -> assertThat(e.getFormattedMessage()).contains("not HTTPS"));
            } finally {
                logger.detachAppender(appender);
            }
        }

    }

    @Nested
    @SpringBootTest(classes = {TbKafkaSettings.class, KafkaAdmin.class})
    @TestPropertySource(properties = {
            "queue.type=kafka",
            "queue.kafka.bootstrap.servers=localhost:9092",
            "queue.kafka.use_confluent_cloud=true",
            "queue.kafka.confluent.sasl.mechanism=OAUTHBEARER",
            "queue.kafka.confluent.security.protocol=SASL_SSL",
            "queue.kafka.confluent.oauth.client-id=my-client",
            "queue.kafka.confluent.oauth.client-secret=my-secret",
            "queue.kafka.confluent.oauth.endpoint-url=https://idp.example.com/oauth/token",
            "queue.kafka.confluent.oauth.scope=api://my-app/.default"
    })
    class ConfluentOAuthScope {

        @Autowired
        TbKafkaSettings settings;

        @Test
        void givenOauthScope_whenToProps_thenAppendsScopeToJaasConfig() {
            Properties props = settings.toProps();

            assertThat(props.getProperty("sasl.jaas.config"))
                    .contains("org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required")
                    .contains("clientId=\"my-client\"")
                    .contains("clientSecret=\"my-secret\"")
                    .contains("scope=\"api://my-app/.default\"")
                    .endsWith("\";");
        }

    }

    @Nested
    @SpringBootTest(classes = {TbKafkaSettings.class, KafkaAdmin.class})
    @TestPropertySource(properties = {
            "queue.type=kafka",
            "queue.kafka.bootstrap.servers=localhost:9092",
            "queue.kafka.use_confluent_cloud=true",
            "queue.kafka.confluent.sasl.mechanism=OAUTHBEARER",
            "queue.kafka.confluent.security.protocol=SASL_SSL",
            "queue.kafka.confluent.oauth.client-id=my-client",
            "queue.kafka.confluent.oauth.client-secret=my-secret"
    })
    class ConfluentOAuthMissingConfig {

        @Autowired
        TbKafkaSettings settings;

        @Test
        void givenOauthbearerWithoutEndpointUrl_whenToProps_thenFailsFast() {
            assertThatThrownBy(() -> settings.toProps())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OAUTHBEARER")
                    .hasMessageContaining("endpoint-url");
        }

    }

    @Nested
    @SpringBootTest(classes = {TbKafkaSettings.class, KafkaAdmin.class})
    @TestPropertySource(properties = {
            "queue.type=kafka",
            "queue.kafka.bootstrap.servers=localhost:9092",
            "queue.kafka.use_confluent_cloud=true",
            "queue.kafka.confluent.sasl.mechanism=PLAIN",
            "queue.kafka.confluent.security.protocol=SASL_SSL",
            "queue.kafka.confluent.sasl.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"key\" password=\"secret\";"
    })
    class ConfluentPlain {

        @Autowired
        TbKafkaSettings settings;

        @Test
        void givenPlainMechanism_whenToProps_thenUsesLegacySaslConfigAndNoOauthKeys() {
            Properties props = settings.toProps();

            assertThat(props).containsEntry("sasl.mechanism", "PLAIN");
            assertThat(props.getProperty("sasl.jaas.config"))
                    .isEqualTo("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"key\" password=\"secret\";");
            assertThat(props).doesNotContainKey("sasl.login.callback.handler.class");
            assertThat(props).doesNotContainKey("sasl.oauthbearer.token.endpoint.url");
        }

    }

}
