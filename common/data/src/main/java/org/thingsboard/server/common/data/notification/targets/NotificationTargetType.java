// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.Arrays;
import java.util.Set;

@RequiredArgsConstructor
public enum NotificationTargetType {

    PLATFORM_USERS(Set.of(NotificationDeliveryMethod.WEB, NotificationDeliveryMethod.EMAIL, NotificationDeliveryMethod.SMS, NotificationDeliveryMethod.MOBILE_APP)),
    SLACK(Set.of(NotificationDeliveryMethod.SLACK)),
    MICROSOFT_TEAMS(Set.of(NotificationDeliveryMethod.MICROSOFT_TEAMS));

    @Getter
    private final Set<NotificationDeliveryMethod> supportedDeliveryMethods;

    public static NotificationTargetType forDeliveryMethod(NotificationDeliveryMethod deliveryMethod) {
        return Arrays.stream(values())
                .filter(targetType -> targetType.getSupportedDeliveryMethods().contains(deliveryMethod))
                .findFirst().orElse(null);
    }

}
