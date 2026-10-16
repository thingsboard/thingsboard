// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.notification;

import org.thingsboard.monitoring.data.Latency;

import java.util.Collection;

public class HighLatencyNotification implements Notification {

    private final Collection<Latency> highLatencies;
    private final int thresholdMs;

    public HighLatencyNotification(Collection<Latency> highLatencies, int thresholdMs) {
        this.highLatencies = highLatencies;
        this.thresholdMs = thresholdMs;
    }

    @Override
    public String getText() {
        StringBuilder text = new StringBuilder();
        text.append("Some of the latencies are higher than ").append(thresholdMs).append(" ms:\n");
        highLatencies.forEach(latency -> {
            text.append(String.format("[%s] *%s*\n", latency.getKey(), latency.getFormattedValue()));
        });
        return text.toString();
    }

}
