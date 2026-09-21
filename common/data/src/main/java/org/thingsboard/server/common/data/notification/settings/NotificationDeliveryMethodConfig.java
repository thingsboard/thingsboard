// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "method")
@JsonSubTypes({
        @Type(name = "SLACK", value = SlackNotificationDeliveryMethodConfig.class),
        @Type(name = "MOBILE_APP", value = MobileAppNotificationDeliveryMethodConfig.class)
})
public interface NotificationDeliveryMethodConfig extends Serializable {

    @JsonIgnore
    NotificationDeliveryMethod getMethod();

}
