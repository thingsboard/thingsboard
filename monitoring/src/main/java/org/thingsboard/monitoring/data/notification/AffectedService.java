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
