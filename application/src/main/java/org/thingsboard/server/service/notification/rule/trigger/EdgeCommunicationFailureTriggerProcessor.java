/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.EdgeCommunicationFailureNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeCommunicationFailureTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeCommunicationFailureNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Service
@RequiredArgsConstructor
public class EdgeCommunicationFailureTriggerProcessor implements NotificationRuleTriggerProcessor<EdgeCommunicationFailureTrigger, EdgeCommunicationFailureNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(EdgeCommunicationFailureTrigger trigger, EdgeCommunicationFailureNotificationRuleTriggerConfig triggerConfig) {
        if (CollectionUtils.isNotEmpty(triggerConfig.getEdges())) {
            return !triggerConfig.getEdges().contains(trigger.getEdgeId().getId());
        }
        return true;
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EdgeCommunicationFailureTrigger trigger) {
        return EdgeCommunicationFailureNotificationInfo.builder()
                .tenantId(trigger.getTenantId())
                .edgeId(trigger.getEdgeId())
                .customerId(trigger.getCustomerId())
                .edgeName(trigger.getEdgeName())
                .failureMsg(truncateFailureMsg(trigger.getFailureMsg()))
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.EDGE_COMMUNICATION_FAILURE;
    }

    private String truncateFailureMsg(String input) {
        int maxLength = 500;
        if (input != null && input.length() > maxLength) {
            return input.substring(0, maxLength);
        }
        return input;
    }
}
