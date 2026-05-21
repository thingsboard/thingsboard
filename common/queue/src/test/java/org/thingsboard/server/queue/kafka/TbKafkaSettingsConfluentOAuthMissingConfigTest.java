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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class TbKafkaSettingsConfluentOAuthMissingConfigTest {

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
