// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SlackDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate {

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody)
    );

    public SlackDeliveryMethodNotificationTemplate(DeliveryMethodNotificationTemplate other) {
        super(other);
    }

    @NoXss(fieldName = "Slack message")
    @Override
    public String getBody() {
        return super.getBody();
    }

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.SLACK;
    }

    @Override
    public SlackDeliveryMethodNotificationTemplate copy() {
        return new SlackDeliveryMethodNotificationTemplate(this);
    }

}
