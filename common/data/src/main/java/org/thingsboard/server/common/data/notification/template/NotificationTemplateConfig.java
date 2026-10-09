// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.notification.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.HashMap;
import java.util.Map;

@Data
public class NotificationTemplateConfig {

    @Valid
    @NotEmpty
    private Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> deliveryMethodsTemplates;

    private boolean attachReport;
    private ReportTemplateId reportTemplateId;
    private UserId userId;
    private String timezone;
    private boolean makePublic;

    public NotificationTemplateConfig copy() {
        Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates = new HashMap<>(deliveryMethodsTemplates);
        templates.replaceAll((deliveryMethod, template) -> template.copy());
        NotificationTemplateConfig copy = new NotificationTemplateConfig();
        copy.setDeliveryMethodsTemplates(templates);
        copy.setAttachReport(attachReport);
        copy.setReportTemplateId(reportTemplateId);
        copy.setUserId(userId);
        copy.setTimezone(timezone);
        copy.setMakePublic(makePublic);
        return copy;
    }

}
