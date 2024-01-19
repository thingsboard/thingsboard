/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.EdgeFailureNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeFailureTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeFailureNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Service
@RequiredArgsConstructor
public class EdgeFailureTriggerProcessor implements NotificationRuleTriggerProcessor<EdgeFailureTrigger, EdgeFailureNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(EdgeFailureTrigger trigger, EdgeFailureNotificationRuleTriggerConfig triggerConfig) {
        if (CollectionUtils.isNotEmpty(triggerConfig.getEdges())) {
            return !triggerConfig.getEdges().contains(trigger.getEdgeId().getId());
        }
        return true;
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EdgeFailureTrigger trigger) {
        return EdgeFailureNotificationInfo.builder()
                .tenantId(trigger.getTenantId())
                .edgeId(trigger.getEdgeId())
                .customerId(trigger.getCustomerId())
                .edgeName(trigger.getEdgeName())
                .errorMsg(trigger.getErrorMsg())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.EDGE_FAILURE;
    }
}
