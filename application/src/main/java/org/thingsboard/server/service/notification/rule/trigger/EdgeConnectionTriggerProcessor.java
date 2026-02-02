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
import org.thingsboard.server.common.data.notification.info.EdgeConnectionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeConnectionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeConnectionNotificationRuleTriggerConfig.EdgeConnectivityEvent;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Service
@RequiredArgsConstructor
public class EdgeConnectionTriggerProcessor implements NotificationRuleTriggerProcessor<EdgeConnectionTrigger, EdgeConnectionNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(EdgeConnectionTrigger trigger, EdgeConnectionNotificationRuleTriggerConfig triggerConfig) {
        EdgeConnectivityEvent event = trigger.isConnected() ? EdgeConnectivityEvent.CONNECTED : EdgeConnectivityEvent.DISCONNECTED;
        if (CollectionUtils.isEmpty(triggerConfig.getNotifyOn()) || !triggerConfig.getNotifyOn().contains(event)) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(triggerConfig.getEdges())) {
            return triggerConfig.getEdges().contains(trigger.getEdgeId().getId());
        }
        return true;
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EdgeConnectionTrigger trigger) {
        return EdgeConnectionNotificationInfo.builder()
                .eventType(trigger.isConnected() ? "connected" : "disconnected")
                .tenantId(trigger.getTenantId())
                .customerId(trigger.getCustomerId())
                .edgeId(trigger.getEdgeId())
                .edgeName(trigger.getEdgeName())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.EDGE_CONNECTION;
    }

}
