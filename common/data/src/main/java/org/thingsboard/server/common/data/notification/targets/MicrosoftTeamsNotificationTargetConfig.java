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
package org.thingsboard.server.common.data.notification.targets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MicrosoftTeamsNotificationTargetConfig extends NotificationTargetConfig implements NotificationRecipient {

    @NotBlank
    private String webhookUrl;
    @NotEmpty
    private String channelName;
    private Boolean useOldApi = Boolean.TRUE;

    @Override
    public NotificationTargetType getType() {
        return NotificationTargetType.MICROSOFT_TEAMS;
    }

    @Override
    public Object getId() {
        return webhookUrl;
    }

    @Override
    public String getTitle() {
        return channelName;
    }

}
