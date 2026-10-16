// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "method")
@JsonSubTypes({
        @Type(name = "WEB", value = WebDeliveryMethodNotificationTemplate.class),
        @Type(name = "EMAIL", value = EmailDeliveryMethodNotificationTemplate.class),
        @Type(name = "SMS", value = SmsDeliveryMethodNotificationTemplate.class),
        @Type(name = "SLACK", value = SlackDeliveryMethodNotificationTemplate.class),
        @Type(name = "MICROSOFT_TEAMS", value = MicrosoftTeamsDeliveryMethodNotificationTemplate.class),
        @Type(name = "MOBILE_APP", value = MobileAppDeliveryMethodNotificationTemplate.class)
})
@Data
@NoArgsConstructor
public abstract class DeliveryMethodNotificationTemplate {

    private boolean enabled;
    @NotEmpty
    protected String body;

    public DeliveryMethodNotificationTemplate(DeliveryMethodNotificationTemplate other) {
        this.enabled = other.enabled;
        this.body = other.body;
    }

    @JsonIgnore
    public abstract NotificationDeliveryMethod getMethod();

    @JsonIgnore
    public abstract DeliveryMethodNotificationTemplate copy();

    @JsonIgnore
    public abstract List<TemplatableValue> getTemplatableValues();

}
