// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.config.transport;

import lombok.Data;

@Data
public class TransportInfo {

    private final TransportType type;
    private final TransportMonitoringTarget target;

    @Override
    public String toString() {
        if (target.getQueue().equals("Main")) {
            return String.format("*%s* (%s)", type.getName(), target.getBaseUrl());
        } else {
            return String.format("*%s* (%s) _%s_", type.getName(), target.getBaseUrl(), target.getQueue());
        }
    }

}
