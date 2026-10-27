// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class UserNotificationSettings {

    @NotNull
    @Valid
    private final Map<NotificationType, NotificationPref> prefs;

    public static final UserNotificationSettings DEFAULT = new UserNotificationSettings(Collections.emptyMap());

    public static final Set<NotificationDeliveryMethod> deliveryMethods = NotificationTargetType.PLATFORM_USERS.getSupportedDeliveryMethods();

    @JsonCreator
    public UserNotificationSettings(@JsonProperty("prefs") Map<NotificationType, NotificationPref> prefs) {
        this.prefs = prefs;
    }

    public boolean isEnabled(NotificationType notificationType, NotificationDeliveryMethod deliveryMethod) {
        NotificationPref pref = prefs.get(notificationType);
        if (pref == null) {
            return true;
        }
        if (!pref.isEnabled()) {
            return false;
        }
        return pref.getEnabledDeliveryMethods().getOrDefault(deliveryMethod, true);
    }

    @Data
    public static class NotificationPref {
        private boolean enabled;
        @NotNull
        private Map<NotificationDeliveryMethod, Boolean> enabledDeliveryMethods;

        public static NotificationPref createDefault() {
            NotificationPref pref = new NotificationPref();
            pref.setEnabled(true);
            pref.setEnabledDeliveryMethods(deliveryMethods.stream().collect(Collectors.toMap(v -> v, v -> true)));
            return pref;
        }

        @JsonIgnore
        @AssertTrue(message = "Only email, Web and SMS delivery methods are allowed")
        public boolean isValid() {
            return enabledDeliveryMethods.entrySet().stream()
                    .allMatch(entry -> deliveryMethods.contains(entry.getKey()) && entry.getValue() != null);
        }
    }

}
