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
package org.thingsboard.server.common.data.notification.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
public class UserNotificationSettings {

    @NotNull
    @Valid
    private final Map<NotificationType, NotificationPref> prefs;

    public static final UserNotificationSettings DEFAULT = new UserNotificationSettings(Collections.emptyMap());

    @JsonCreator
    public UserNotificationSettings(@JsonProperty("prefs") Map<NotificationType, NotificationPref> prefs) {
        this.prefs = prefs;
    }

    public Set<NotificationDeliveryMethod> getEnabledDeliveryMethods(NotificationType notificationType) {
        NotificationPref pref = prefs.get(notificationType);
        if (pref != null) {
            return pref.isEnabled() ? pref.getEnabledDeliveryMethods() : Collections.emptySet();
        } else {
            return NotificationDeliveryMethod.values;
        }
    }

    @Data
    public static class NotificationPref {
        private boolean enabled;
        @NotNull
        private Set<NotificationDeliveryMethod> enabledDeliveryMethods;

        public static NotificationPref createDefault() {
            NotificationPref pref = new NotificationPref();
            pref.setEnabled(true);
            pref.setEnabledDeliveryMethods(NotificationDeliveryMethod.values);
            return pref;
        }

        @JsonIgnore
        @AssertTrue(message = "Only email, Web and SMS delivery methods are allowed")
        public boolean isValid() {
            return NotificationTargetType.PLATFORM_USERS.getSupportedDeliveryMethods().containsAll(enabledDeliveryMethods);
        }
    }

}
