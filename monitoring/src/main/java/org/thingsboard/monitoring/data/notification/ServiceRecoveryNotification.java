// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.notification;

public class ServiceRecoveryNotification implements Notification {

    private final Object serviceKey;

    public ServiceRecoveryNotification(Object serviceKey) {
        this.serviceKey = serviceKey;
    }

    @Override
    public String getText() {
        return String.format("%s is OK", serviceKey);
    }

}
