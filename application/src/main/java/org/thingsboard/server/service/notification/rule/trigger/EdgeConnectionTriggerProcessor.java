// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
