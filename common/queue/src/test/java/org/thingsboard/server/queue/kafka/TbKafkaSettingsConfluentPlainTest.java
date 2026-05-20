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
        "queue.kafka.confluent.sasl.mechanism=PLAIN",
        "queue.kafka.confluent.security.protocol=SASL_SSL",
        "queue.kafka.confluent.sasl.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"key\" password=\"secret\";"
})
class TbKafkaSettingsConfluentPlainTest {

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
