/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Data
public class NotificationTemplateConfig {

    private String notificationSubject;
    private String defaultTextTemplate;
    @Valid
    @NotEmpty
    private Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> deliveryMethodsTemplates;

    @JsonIgnore
    @AssertTrue(message = "defaultTextTemplate and notificationSubject must be specified if one absent for delivery method")
    public boolean isValid() {
        if (deliveryMethodsTemplates == null) return true;
        for (DeliveryMethodNotificationTemplate template : deliveryMethodsTemplates.values()) {
            if (StringUtils.isEmpty(template.getBody()) && StringUtils.isEmpty(defaultTextTemplate)) {
                return false;
            }
            if (template instanceof HasSubject) {
                String subject = ((HasSubject) template).getSubject();
                if (StringUtils.isEmpty(subject) && StringUtils.isEmpty(notificationSubject)) {
                    return false;
                }
            }
        }
        return true;
    }

}
