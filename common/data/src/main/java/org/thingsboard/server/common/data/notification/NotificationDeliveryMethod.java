// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NotificationDeliveryMethod {

    WEB("web"),
    EMAIL("email"),
    SMS("SMS"),
    SLACK("Slack"),
    MICROSOFT_TEAMS("Microsoft Teams"),
    MOBILE_APP("mobile app");

    @Getter
    private final String name;

}
