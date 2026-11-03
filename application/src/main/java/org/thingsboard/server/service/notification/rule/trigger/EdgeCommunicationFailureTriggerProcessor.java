// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
