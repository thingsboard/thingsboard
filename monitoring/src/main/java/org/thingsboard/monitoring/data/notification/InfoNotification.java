// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.notification;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InfoNotification implements Notification {
    private final String message;
    @Override
    public String getText() {
        return message;
    }

    @Override
    public boolean isIncident() {
        return false;
    }
}
