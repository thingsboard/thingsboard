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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class MicrosoftTeamsDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    private String subject;
    private String themeColor;
    private Button button;

    private String customMessageCardJson;

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody),
            TemplatableValue.of(this::getSubject, this::setSubject),
            TemplatableValue.of(() -> button != null ? button.getName() : null,
                    processed -> {if (button != null) button.setName(processed);}),
            TemplatableValue.of(() -> button != null ? button.getUri() : null,
                    processed -> {if (button != null) button.setUri(processed);}),
            TemplatableValue.of(this::getCustomMessageCardJson, this::setCustomMessageCardJson)
    );

    public MicrosoftTeamsDeliveryMethodNotificationTemplate(MicrosoftTeamsDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
        this.themeColor = other.themeColor;
        this.button = other.button != null ? new Button(other.button) : null;
        this.customMessageCardJson = other.customMessageCardJson;
    }

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.MICROSOFT_TEAMS;
    }

    @Override
    public MicrosoftTeamsDeliveryMethodNotificationTemplate copy() {
        return new MicrosoftTeamsDeliveryMethodNotificationTemplate(this);
    }

    @Data
    @NoArgsConstructor
    public static class Button {
        private String name;
        private String uri;

        public Button(Button other) {
            this.name = other.name;
            this.uri = other.uri;
        }
    }

}
