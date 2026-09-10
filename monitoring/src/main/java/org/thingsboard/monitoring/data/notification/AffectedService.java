// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.notification;

public record AffectedService(String name, Status status, int failureCount) {

    public enum Status { FAILING, RECOVERED, HIGH_LATENCY }

    public static AffectedService failing(String name, int failureCount) {
        return new AffectedService(name, Status.FAILING, failureCount);
    }

    public static AffectedService recovered(String name) {
        return new AffectedService(name, Status.RECOVERED, 0);
    }

    public static AffectedService highLatency(String name) {
        return new AffectedService(name, Status.HIGH_LATENCY, 0);
    }

}
