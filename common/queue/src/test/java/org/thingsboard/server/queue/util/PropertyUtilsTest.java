// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
