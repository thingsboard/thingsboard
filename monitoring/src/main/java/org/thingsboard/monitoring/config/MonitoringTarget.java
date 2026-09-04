// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.config;

import java.util.UUID;

public interface MonitoringTarget {

    UUID getDeviceId();

    String getBaseUrl();

    default String getQueue() {
        return "Main";
    }

    boolean isCheckDomainIps();

}
