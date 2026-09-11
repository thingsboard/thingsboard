// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.template;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EmailDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    @NoXss(fieldName = "email subject")
    @Length(fieldName = "email subject", max = 250, message = "cannot be longer than 250 chars")
    @NotEmpty
    private String subject;

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody),
            TemplatableValue.of(this::getSubject, this::setSubject)
    );

    public EmailDeliveryMethodNotificationTemplate(EmailDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
    }

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.EMAIL;
    }

    @Override
    public EmailDeliveryMethodNotificationTemplate copy() {
        return new EmailDeliveryMethodNotificationTemplate(this);
    }

}
