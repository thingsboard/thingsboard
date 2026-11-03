// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.settings;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

@Data
public class MobileAppNotificationDeliveryMethodConfig implements NotificationDeliveryMethodConfig {

    private String firebaseServiceAccountCredentialsFileName;
    @NotEmpty
    private String firebaseServiceAccountCredentials;

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.MOBILE_APP;
    }

}
