// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.List;

@Schema(
        description = "Base template for different delivery methods",
        discriminatorProperty = "method",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "WEB", schema = WebDeliveryMethodNotificationTemplate.class),
                @DiscriminatorMapping(value = "EMAIL", schema = EmailDeliveryMethodNotificationTemplate.class),
                @DiscriminatorMapping(value = "SMS", schema = SmsDeliveryMethodNotificationTemplate.class),
                @DiscriminatorMapping(value = "SLACK", schema = SlackDeliveryMethodNotificationTemplate.class),
                @DiscriminatorMapping(value = "MICROSOFT_TEAMS", schema = MicrosoftTeamsDeliveryMethodNotificationTemplate.class),
                @DiscriminatorMapping(value = "MOBILE_APP", schema = MobileAppDeliveryMethodNotificationTemplate.class)
        }
)
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
