// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

@Schema
@Data
public class SlackNotificationDeliveryMethodConfig implements NotificationDeliveryMethodConfig {

    @NotEmpty
    private String botToken;

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.SLACK;
    }

}
