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
package org.thingsboard.server.queue.pubsub;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.PropertyUtils;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
public class TbPubSubSubscriptionSettings {
    @Value("${queue.pubsub.queue-properties.core:}")
    private String coreProperties;
    @Value("${queue.pubsub.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    @Value("${queue.pubsub.queue-properties.transport-api:}")
    private String transportApiProperties;
    @Value("${queue.pubsub.queue-properties.notifications:}")
    private String notificationsProperties;
    @Value("${queue.pubsub.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    @Value("${queue.pubsub.queue-properties.version-control:}")
    private String vcProperties;

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
    @Getter
    private Map<String, String> vcSettings;

    @PostConstruct
    private void init() {
        coreSettings = PropertyUtils.getProps(coreProperties);
        ruleEngineSettings = PropertyUtils.getProps(ruleEngineProperties);
        transportApiSettings = PropertyUtils.getProps(transportApiProperties);
        notificationsSettings = PropertyUtils.getProps(notificationsProperties);
        jsExecutorSettings = PropertyUtils.getProps(jsExecutorProperties);
        vcSettings = PropertyUtils.getProps(vcProperties);
    }

}
