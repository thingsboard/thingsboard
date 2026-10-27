// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.HashMap;
import java.util.Map;

@Data
public class NotificationTemplateConfig {

    @Valid
    @NotEmpty
    private Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> deliveryMethodsTemplates;

    public NotificationTemplateConfig copy() {
        Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates = new HashMap<>(deliveryMethodsTemplates);
        templates.replaceAll((deliveryMethod, template) -> template.copy());
        NotificationTemplateConfig copy = new NotificationTemplateConfig();
        copy.setDeliveryMethodsTemplates(templates);
        return copy;
    }

}
