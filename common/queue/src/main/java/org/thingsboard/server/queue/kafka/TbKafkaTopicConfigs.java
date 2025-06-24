/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
public class TbKafkaTopicConfigs {

    public static final String NUM_PARTITIONS_SETTING = "partitions";

    @Value("${queue.kafka.topic-properties.core:}")
    private String coreProperties;
    @Value("${queue.kafka.topic-properties.rule-engine:}")
    private String ruleEngineProperties;
    @Value("${queue.kafka.topic-properties.transport-api:}")
    private String transportApiProperties;
    @Value("${queue.kafka.topic-properties.notifications:}")
    private String notificationsProperties;
    @Value("${queue.kafka.topic-properties.js-executor:}")
    private String jsExecutorProperties;
    @Value("${queue.kafka.topic-properties.ota-updates:}")
    private String fwUpdatesProperties;
    @Value("${queue.kafka.topic-properties.version-control:}")
    private String vcProperties;
    @Value("${queue.kafka.topic-properties.edge:}")
    private String edgeProperties;
    @Value("${queue.kafka.topic-properties.edge-event:}")
    private String edgeEventProperties;
    @Value("${queue.kafka.topic-properties.housekeeper:}")
    private String housekeeperProperties;
    @Value("${queue.kafka.topic-properties.housekeeper-reprocessing:}")
    private String housekeeperReprocessingProperties;
    @Value("${queue.kafka.topic-properties.calculated-field:}")
    private String calculatedFieldProperties;
    @Value("${queue.kafka.topic-properties.calculated-field-state:}")
    private String calculatedFieldStateProperties;
    @Value("${queue.kafka.topic-properties.edqs-events:}")
    private String edqsEventsProperties;
    @Value("${queue.kafka.topic-properties.edqs-requests:}")
    private String edqsRequestsProperties;
    @Value("${queue.kafka.topic-properties.edqs-state:}")
    private String edqsStateProperties;
    @Value("${queue.kafka.topic-properties.tasks:}")
    private String tasksProperties;

    @Getter
    private Map<String, String> coreConfigs;
    @Getter
    private Map<String, String> ruleEngineConfigs;
    @Getter
    private Map<String, String> transportApiRequestConfigs;
    @Getter
    private Map<String, String> transportApiResponseConfigs;
    @Getter
    private Map<String, String> notificationsConfigs;
    @Getter
    private Map<String, String> jsExecutorRequestConfigs;
    @Getter
    private Map<String, String> jsExecutorResponseConfigs;
    @Getter
    private Map<String, String> fwUpdatesConfigs;
    @Getter
    private Map<String, String> vcConfigs;
    @Getter
    private Map<String, String> housekeeperConfigs;
    @Getter
    private Map<String, String> housekeeperReprocessingConfigs;
    @Getter
    private Map<String, String> edgeConfigs;
    @Getter
    private Map<String, String> edgeEventConfigs;
    @Getter
    private Map<String, String> calculatedFieldConfigs;
    @Getter
    private Map<String, String> calculatedFieldStateConfigs;
    @Getter
    private Map<String, String> edqsEventsConfigs;
    @Getter
    private Map<String, String> edqsRequestsConfigs;
    @Getter
    private Map<String, String> edqsStateConfigs;
    @Getter
    private Map<String, String> tasksConfigs;

    @PostConstruct
    private void init() {
        coreConfigs = PropertyUtils.getProps(coreProperties);
        ruleEngineConfigs = PropertyUtils.getProps(ruleEngineProperties);
        transportApiRequestConfigs = PropertyUtils.getProps(transportApiProperties);
        transportApiResponseConfigs = PropertyUtils.getProps(transportApiProperties);
        transportApiResponseConfigs.put(NUM_PARTITIONS_SETTING, "1");
        notificationsConfigs = PropertyUtils.getProps(notificationsProperties);
        jsExecutorRequestConfigs = PropertyUtils.getProps(jsExecutorProperties);
        jsExecutorResponseConfigs = PropertyUtils.getProps(jsExecutorProperties);
        jsExecutorResponseConfigs.put(NUM_PARTITIONS_SETTING, "1");
        fwUpdatesConfigs = PropertyUtils.getProps(fwUpdatesProperties);
        vcConfigs = PropertyUtils.getProps(vcProperties);
        housekeeperConfigs = PropertyUtils.getProps(housekeeperProperties);
        housekeeperReprocessingConfigs = PropertyUtils.getProps(housekeeperReprocessingProperties);
        edgeConfigs = PropertyUtils.getProps(edgeProperties);
        edgeEventConfigs = PropertyUtils.getProps(edgeEventProperties);
        calculatedFieldConfigs = PropertyUtils.getProps(calculatedFieldProperties);
        calculatedFieldStateConfigs = PropertyUtils.getProps(calculatedFieldStateProperties);
        edqsEventsConfigs = PropertyUtils.getProps(edqsEventsProperties);
        edqsRequestsConfigs = PropertyUtils.getProps(edqsRequestsProperties);
        edqsStateConfigs = PropertyUtils.getProps(edqsStateProperties);
        tasksConfigs = PropertyUtils.getProps(tasksProperties);
    }

}
