package org.thingsboard.server.common.data.notification.targets;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
@EqualsAndHashCode(callSuper = true)
public class MicrosoftTeamsNotificationTargetConfig extends NotificationTargetConfig implements NotificationRecipient {

    @NotBlank
    private String webhookUrl;
    @NotEmpty
    private String channelName;

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
