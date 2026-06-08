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
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

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
class TbKafkaSettingsConfluentOAuthHttpWarningTest {

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
