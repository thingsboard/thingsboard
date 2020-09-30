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
package org.thingsboard.server.queue.settings;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
public class TbPubSubSubscriptionSettings {
    @Value("${queue.pubsub.queue-properties.core}")
    private String coreProperties;
    @Value("${queue.pubsub.queue-properties.rule-engine}")
    private String ruleEngineProperties;
    @Value("${queue.pubsub.queue-properties.transport-api}")
    private String transportApiProperties;
    @Value("${queue.pubsub.queue-properties.notifications}")
    private String notificationsProperties;
    @Value("${queue.pubsub.queue-properties.js-executor}")
    private String jsExecutorProperties;

    @Getter
    private Map<String, String> coreSettings;
    @Getter
    private Map<String, String> ruleEngineSettings;
    @Getter
    private Map<String, String> transportApiSettings;
    @Getter
    private Map<String, String> notificationsSettings;
    @Getter
    private Map<String, String> jsExecutorSettings;

    @PostConstruct
    private void init() {
        coreSettings = getSettings(coreProperties);
        ruleEngineSettings = getSettings(ruleEngineProperties);
        transportApiSettings = getSettings(transportApiProperties);
        notificationsSettings = getSettings(notificationsProperties);
        jsExecutorSettings = getSettings(jsExecutorProperties);
    }

    private Map<String, String> getSettings(String properties) {
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
