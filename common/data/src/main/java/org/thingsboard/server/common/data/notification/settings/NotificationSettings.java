// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.io.Serializable;
import java.util.Map;

@Schema
@Data
public class NotificationSettings implements Serializable {

    @NotNull
    @Valid
    private Map<NotificationDeliveryMethod, NotificationDeliveryMethodConfig> deliveryMethodsConfigs;

}
