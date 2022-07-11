/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbRuleEngineQueueConfiguration;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultQueueService implements QueueService {

    private final TbQueueRuleEngineSettings ruleEngineSettings;
    private Set<String> ruleEngineQueues;

    @PostConstruct
    public void init() {
        ruleEngineQueues = ruleEngineSettings.getQueues().stream()
                .map(TbRuleEngineQueueConfiguration::getName).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<String> getQueuesByServiceType(ServiceType type) {
        if (type == ServiceType.TB_RULE_ENGINE) {
            return ruleEngineQueues;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public String resolve(ServiceType serviceType, String queueName) {
        if (StringUtils.isEmpty(queueName) || !getQueuesByServiceType(serviceType).contains(queueName)) {
            return ServiceQueue.MAIN;
        } else {
            return queueName;
        }
    }
}
