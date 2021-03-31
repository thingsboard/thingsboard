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
package org.thingsboard.server.queue.settings;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
public class TbKafkaTopicConfigs {
    @Value("${queue.kafka.topic-properties.core}")
    private String coreProperties;
    @Value("${queue.kafka.topic-properties.rule-engine}")
    private String ruleEngineProperties;
    @Value("${queue.kafka.topic-properties.transport-api}")
    private String transportApiProperties;
    @Value("${queue.kafka.topic-properties.notifications}")
    private String notificationsProperties;
    @Value("${queue.kafka.topic-properties.js-executor}")
    private String jsExecutorProperties;

    @Getter
    private Map<String, String> coreConfigs;
    @Getter
    private Map<String, String> ruleEngineConfigs;
    @Getter
    private Map<String, String> transportApiConfigs;
    @Getter
    private Map<String, String> notificationsConfigs;
    @Getter
    private Map<String, String> jsExecutorConfigs;

    @PostConstruct
    private void init() {
        coreConfigs = getConfigs(coreProperties);
        ruleEngineConfigs = getConfigs(ruleEngineProperties);
        transportApiConfigs = getConfigs(transportApiProperties);
        notificationsConfigs = getConfigs(notificationsProperties);
        jsExecutorConfigs = getConfigs(jsExecutorProperties);
    }

    private Map<String, String> getConfigs(String properties) {
        Map<String, String> configs = new HashMap<>();
        for (String property : properties.split(";")) {
            int delimiterPosition = property.indexOf(":");
            String key = property.substring(0, delimiterPosition);
            String value = property.substring(delimiterPosition + 1);
            configs.put(key, value);
        }
        return configs;
    }
}
