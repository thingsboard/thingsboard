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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.util.CollectionsUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
public class UserNotificationSettings {

    private final Map<NotificationType, NotificationTypePrefs> prefs;

    @JsonCreator
    public UserNotificationSettings(@JsonProperty("prefs") Map<NotificationType, NotificationTypePrefs> prefs) {
        this.prefs = prefs;
    }

    public static final UserNotificationSettings DEFAULT = new UserNotificationSettings(Collections.emptyMap());

    public Set<NotificationDeliveryMethod> getEnabledDeliveryMethods(NotificationType notificationType) {
        NotificationTypePrefs prefs;
        if (this.prefs == null || (prefs = this.prefs.get(notificationType)) == null) {
            return NotificationDeliveryMethod.values;
        }
        if (prefs.isEnabled()) {
            Set<NotificationDeliveryMethod> deliveryMethods = prefs.getEnabledDeliveryMethods();
            if (CollectionsUtil.isNotEmpty(deliveryMethods)) {
                return deliveryMethods;
            } else {
                return NotificationDeliveryMethod.values;
            }
        } else {
            return Collections.emptySet();
        }
    }

    @Data
    public static class NotificationTypePrefs {
        private boolean enabled;
        private Set<NotificationDeliveryMethod> enabledDeliveryMethods;
    }

}
