// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationRequestInfo extends NotificationRequest {

    private String templateName;
    private List<NotificationDeliveryMethod> deliveryMethods;

    public NotificationRequestInfo(NotificationRequest request, String templateName, List<NotificationDeliveryMethod> deliveryMethods) {
        super(request);
        this.templateName = templateName;
        this.deliveryMethods = deliveryMethods;
    }

}
