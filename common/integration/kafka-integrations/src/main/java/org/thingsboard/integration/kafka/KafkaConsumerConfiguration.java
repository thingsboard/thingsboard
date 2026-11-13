// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.integration.template.TemplateField;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaConsumerConfiguration {

    @TemplateField(key = "kafkaClientId", label = "Client ID",
                   group = "Kafka Connection")
    private String clientId;

    @TemplateField(key = "kafkaGroupId", label = "Consumer group ID",
                   group = "Kafka Connection", required = true)
    private String groupId;

    @TemplateField(key = "kafkaTopics", label = "Topics",
                   group = "Kafka Connection", required = true)
    private String topics;

    @TemplateField(key = "kafkaBootstrapServers", label = "Bootstrap servers",
                   group = "Kafka Connection", required = true)
    private String bootstrapServers;

    private String autoCreateTopics;
    private long pollInterval;
    private Map<String, String> otherProperties;
}
