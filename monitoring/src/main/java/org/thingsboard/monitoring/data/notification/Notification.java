// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.notification;

import java.util.List;

public interface Notification {

    String getText();

    default boolean isIncident() {
        return true;
    }

    default List<AffectedService> getAffectedServices() {
        return List.of();
    }

}
