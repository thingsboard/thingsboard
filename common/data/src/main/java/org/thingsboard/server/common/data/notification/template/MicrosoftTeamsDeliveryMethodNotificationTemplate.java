/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class MicrosoftTeamsDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    private String subject;
    private String themeColor;
    private Button button;

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody),
            TemplatableValue.of(this::getSubject, this::setSubject),
            TemplatableValue.of(() -> button != null ? button.getText() : null,
                    processed -> { if (button != null) button.setText(processed); }),
            TemplatableValue.of(() -> button != null ? button.getLink() : null,
                    processed -> { if (button != null) button.setLink(processed); })
    );

    public MicrosoftTeamsDeliveryMethodNotificationTemplate(MicrosoftTeamsDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
        this.themeColor = other.themeColor;
        this.button = other.button != null ? new Button(other.button) : null;
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
        private boolean enabled;
        private String text;
        private LinkType linkType;
        private String link;

        private UUID dashboardId;
        private String dashboardState;
        private boolean setEntityIdInState;

        public Button(Button other) {
            this.enabled = other.enabled;
            this.text = other.text;
            this.linkType = other.linkType;
            this.link = other.link;
            this.dashboardId = other.dashboardId;
            this.dashboardState = other.dashboardState;
            this.setEntityIdInState = other.setEntityIdInState;
        }

        public enum LinkType {
            LINK, DASHBOARD
        }
    }

}
