// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.config.transport;

import lombok.Data;
import org.thingsboard.monitoring.data.notification.ShortNameProvider;

@Data
public class TransportInfo implements ShortNameProvider {

    private final TransportType type;
    private final TransportMonitoringTarget target;

    public String getShortName() {
        if (target.getQueue().equals("Main")) {
            return type.getName();
        }
        return type.getName() + " " + target.getQueue();
    }

    @Override
    public String toString() {
        if (target.getQueue().equals("Main")) {
            return String.format("*%s* (%s)", type.getName(), target.getBaseUrl());
        } else {
            return String.format("*%s* (%s) _%s_", type.getName(), target.getBaseUrl(), target.getQueue());
        }
    }

}
