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

import org.junit.jupiter.api.Test;
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
        "queue.kafka.confluent.security.protocol=SASL_SSL",
        "queue.kafka.confluent.oauth.client-id=client\"with\\\\quote",
        "queue.kafka.confluent.oauth.client-secret=secret\"with\\\\backslash;and-semi",
        "queue.kafka.confluent.oauth.endpoint-url=https://idp.example.com/oauth/token"
})
class TbKafkaSettingsConfluentOAuthEscapingTest {

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
