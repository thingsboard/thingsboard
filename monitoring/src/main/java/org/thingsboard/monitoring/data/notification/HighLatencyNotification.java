/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
