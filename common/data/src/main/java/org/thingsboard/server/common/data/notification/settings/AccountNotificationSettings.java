package org.thingsboard.server.common.data.notification.settings;

import lombok.Data;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.Set;

@Data
public class AccountNotificationSettings {

    private UserId userId;
    private Set<NotificationDeliveryMethod> allowedNotifications;

}
