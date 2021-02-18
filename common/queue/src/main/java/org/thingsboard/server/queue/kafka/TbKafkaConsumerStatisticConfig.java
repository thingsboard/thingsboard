/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
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
