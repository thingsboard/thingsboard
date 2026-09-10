// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbKafkaComponent;

@Component
@TbKafkaComponent
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TbKafkaConsumerStatisticConfig {
    @Value("${queue.kafka.consumer-stats.enabled:true}")
    private Boolean enabled;
    @Value("${queue.kafka.consumer-stats.print-interval-ms:60000}")
    private Long printIntervalMs;
    @Value("${queue.kafka.consumer-stats.kafka-response-timeout-ms:1000}")
    private Long kafkaResponseTimeoutMs;
}
