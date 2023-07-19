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
package org.thingsboard.server.queue.azure.servicebus;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.PropertyUtils;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='service-bus'")
public class TbServiceBusQueueConfigs {
    @Value("${queue.service-bus.queue-properties.core:}")
    private String coreProperties;
    @Value("${queue.service-bus.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    @Value("${queue.service-bus.queue-properties.transport-api:}")
    private String transportApiProperties;
    @Value("${queue.service-bus.queue-properties.notifications:}")
    private String notificationsProperties;
    @Value("${queue.service-bus.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    @Value("${queue.service-bus.queue-properties.version-control:}")
    private String vcProperties;
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
    @Getter
    private Map<String, String> vcConfigs;

    @PostConstruct
    private void init() {
        coreConfigs = PropertyUtils.getProps(coreProperties);
        ruleEngineConfigs = PropertyUtils.getProps(ruleEngineProperties);
        transportApiConfigs = PropertyUtils.getProps(transportApiProperties);
        notificationsConfigs = PropertyUtils.getProps(notificationsProperties);
        jsExecutorConfigs = PropertyUtils.getProps(jsExecutorProperties);
        vcConfigs = PropertyUtils.getProps(vcProperties);
    }

}
