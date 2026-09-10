// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationRequestInfo extends NotificationRequest {

    @Schema
    private String templateName;
    @ArraySchema(schema = @Schema(implementation = NotificationDeliveryMethod.class))
    private List<NotificationDeliveryMethod> deliveryMethods;

    public NotificationRequestInfo(NotificationRequest request, String templateName, List<NotificationDeliveryMethod> deliveryMethods) {
        super(request);
        this.templateName = templateName;
        this.deliveryMethods = deliveryMethods;
    }

}
