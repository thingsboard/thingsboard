/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Component("ruleEngineTbKafkaTopicConfig")
@ConfigurationProperties(prefix = "queue.kafka.topic-properties.rule-engine")
public class RuleEngineTbKafkaTopicConfig implements TbKafkaTopicConfig {

    private List<TbKafkaProperty> properties;

    private final Map<String, String> configs = new HashMap<>();

    @PostConstruct
    private void init() {
        properties.forEach(p -> configs.put(p.getKey(), p.getValue()));
    }
}
