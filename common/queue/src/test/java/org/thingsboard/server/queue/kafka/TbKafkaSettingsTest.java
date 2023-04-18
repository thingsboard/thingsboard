/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

@SpringBootTest(classes = TbKafkaSettings.class)
@TestPropertySource(properties = {
        "queue.type=kafka",
        "queue.kafka.bootstrap.servers=localhost:9092",
        "queue.kafka.other-inline=metrics.recording.level:INFO;metrics.sample.window.ms:30000",
})
class TbKafkaSettingsTest {

    @Autowired
    TbKafkaSettings settings;

    @BeforeEach
    void beforeEach() {
        settings = spy(settings); //SpyBean is not aware on @ConditionalOnProperty, that is why the traditional spy in use
    }

    @Test
    void givenToProps_whenConfigureSSL_thenVerifyOnce() {
        Properties props = settings.toProps();

        assertThat(props).as("TB_QUEUE_KAFKA_REQUEST_TIMEOUT_MS").containsEntry("request.timeout.ms", 30000);
        assertThat(props).as("TB_QUEUE_KAFKA_SESSION_TIMEOUT_MS").containsEntry("session.timeout.ms", 10000);

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

}