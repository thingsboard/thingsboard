// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.template;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MobileAppDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    @Schema(description = "Subject line for the mobile notification", example = "New Message Received")
    @NotEmpty
    private String subject;
    @Schema(description = "Additional JSON configuration for web buttons/actions", type = "object")
    private JsonNode additionalConfig;

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody),
            TemplatableValue.of(this::getSubject, this::setSubject)
    );

    public MobileAppDeliveryMethodNotificationTemplate(MobileAppDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
        this.additionalConfig = other.additionalConfig;
    }

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.MOBILE_APP;
    }

    @Override
    public MobileAppDeliveryMethodNotificationTemplate copy() {
        return new MobileAppDeliveryMethodNotificationTemplate(this);
    }

    @Override
    public List<TemplatableValue> getTemplatableValues() {
        return templatableValues;
    }

}
