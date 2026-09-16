// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.rule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationRuleInfo extends NotificationRule {

    private String templateName;
    private List<NotificationDeliveryMethod> deliveryMethods;

    public NotificationRuleInfo(NotificationRule rule, String templateName, List<NotificationDeliveryMethod> deliveryMethods) {
        super(rule);
        this.templateName = templateName;
        this.deliveryMethods = deliveryMethods;
    }

}
