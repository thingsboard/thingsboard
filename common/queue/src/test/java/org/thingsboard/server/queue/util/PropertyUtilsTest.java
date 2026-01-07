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
package org.thingsboard.server.queue.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyUtilsTest {

    @Test
    void givenNullOrEmpty_whenGetConfig_thenEmptyMap() {
        assertThat(PropertyUtils.getProps(null)).as("null property").isEmpty();
        assertThat(PropertyUtils.getProps("")).as("empty property").isEmpty();
        assertThat(PropertyUtils.getProps(";")).as("ends with ;").isEmpty();
    }

    @Test
    void givenKafkaOtherProperties_whenGetConfig_thenReturnMappedValues() {
        assertThat(PropertyUtils.getProps("metrics.recording.level:INFO;metrics.sample.window.ms:30000"))
                .as("two pairs")
                .isEqualTo(Map.of(
                        "metrics.recording.level", "INFO",
                        "metrics.sample.window.ms", "30000"
                ));

        assertThat(PropertyUtils.getProps("metrics.recording.level:INFO;metrics.sample.window.ms:30000" + ";"))
                .as("two pairs ends with ;")
                .isEqualTo(Map.of(
                        "metrics.recording.level", "INFO",
                        "metrics.sample.window.ms", "30000"
                ));
    }

    @Test
    void givenKafkaTopicProperties_whenGetConfig_thenReturnMappedValues() {
        assertThat(PropertyUtils.getProps("retention.ms:604800000;segment.bytes:52428800;retention.bytes:1048576000;partitions:1;min.insync.replicas:1"))
                .isEqualTo(Map.of(
                        "retention.ms", "604800000",
                        "segment.bytes", "52428800",
                        "retention.bytes", "1048576000",
                        "partitions", "1",
                        "min.insync.replicas", "1"
                ));
    }

}
