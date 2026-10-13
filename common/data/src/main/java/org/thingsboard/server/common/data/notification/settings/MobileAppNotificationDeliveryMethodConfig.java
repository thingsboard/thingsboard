// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.notification.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

@Schema
@Data
public class MobileAppNotificationDeliveryMethodConfig implements NotificationDeliveryMethodConfig {

    private String firebaseServiceAccountCredentialsFileName;
    private String firebaseServiceAccountCredentials;
    private boolean useSystemSettings;

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.MOBILE_APP;
    }

    @JsonIgnore
    @AssertTrue(message = "Firebase service account credentials must be specified")
    public boolean isValid() {
        if (useSystemSettings) {
            return true;
        }
        return StringUtils.isNotEmpty(firebaseServiceAccountCredentials);
    }

}
