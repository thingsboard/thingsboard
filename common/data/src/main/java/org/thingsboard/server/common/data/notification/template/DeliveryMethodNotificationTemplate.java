/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.notification.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "method")
@JsonSubTypes({
        @Type(name = "PUSH", value = PushDeliveryMethodNotificationTemplate.class),
        @Type(name = "EMAIL", value = EmailDeliveryMethodNotificationTemplate.class),
        @Type(name = "SMS", value = SmsDeliveryMethodNotificationTemplate.class),
        @Type(name = "SLACK", value = SlackDeliveryMethodNotificationTemplate.class)
})
@Data
public abstract class DeliveryMethodNotificationTemplate {

    private String body;

    public abstract NotificationDeliveryMethod getMethod();

}
