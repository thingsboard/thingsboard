// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.thingsboard.server.common.data.notification.targets.platform.PlatformUsersNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.targets.slack.SlackNotificationTargetConfig;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = PlatformUsersNotificationTargetConfig.class, name = "PLATFORM_USERS"),
        @Type(value = SlackNotificationTargetConfig.class, name = "SLACK"),
        @Type(value = MicrosoftTeamsNotificationTargetConfig.class, name = "MICROSOFT_TEAMS")
})
@Data
public abstract class NotificationTargetConfig {

    @NoXss
    @Length(max = 500, message = "cannot be longer than 500 chars")
    private String description;

    @JsonIgnore
    public abstract NotificationTargetType getType();

}
