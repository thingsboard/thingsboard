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
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
public class UserNotificationSettings {

    @NotNull
    @Valid
    private final List<NotificationPref> prefs;

    public static final UserNotificationSettings DEFAULT = new UserNotificationSettings(Collections.emptyList());

    @JsonCreator
    public UserNotificationSettings(@JsonProperty("prefs") List<NotificationPref> prefs) {
        this.prefs = prefs;
    }

    public Set<NotificationDeliveryMethod> getEnabledDeliveryMethods(NotificationRuleId ruleId) {
        return prefs.stream()
                .filter(pref -> pref.getRuleId().equals(ruleId.getId())).findFirst()
                .map(pref -> pref.isEnabled() ? pref.getEnabledDeliveryMethods() : Collections.<NotificationDeliveryMethod>emptySet())
                .orElse(NotificationDeliveryMethod.values);
    }

    @Data
    public static class NotificationPref {
        @NotNull
        private UUID ruleId;
        private String ruleName;
        private boolean enabled;
        @NotNull
        private Set<NotificationDeliveryMethod> enabledDeliveryMethods;

        public static NotificationPref createDefault(NotificationRule rule) {
            NotificationPref pref = new NotificationPref();
            pref.setRuleId(rule.getUuidId());
            pref.setRuleName(rule.getName());
            pref.setEnabled(true);
            pref.setEnabledDeliveryMethods(NotificationDeliveryMethod.values);
            return pref;
        }
    }

}
