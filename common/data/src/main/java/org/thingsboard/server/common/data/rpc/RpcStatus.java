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
package org.thingsboard.server.common.data.rpc;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum RpcStatus {

    QUEUED(true),
    SENT(true),
    DELIVERED(true),
    SUCCESSFUL(false),
    TIMEOUT(false),
    EXPIRED(false),
    FAILED(false),
    DELETED(false);

    // Precomputed once per kind: a pure function of (target status, kind), read on every status write.
    private static final Map<RpcStatus, Set<RpcStatus>> ALLOWED_FROM_ONE_WAY = allowedFrom(RpcKind.ONE_WAY);
    private static final Map<RpcStatus, Set<RpcStatus>> ALLOWED_FROM_TWO_WAY = allowedFrom(RpcKind.TWO_WAY);

    @Getter
    private final boolean pushDeleteNotificationToCore;

    RpcStatus(boolean pushDeleteNotificationToCore) {
        this.pushDeleteNotificationToCore = pushDeleteNotificationToCore;
    }

    /**
     * The set of CURRENT (persisted) statuses that a guarded status UPDATE to THIS (target) status may overwrite.
     * No terminal status appears in any set, so terminals are immutable. TIMEOUT is an in-flight peer of SENT
     * (delivery ack timed out, retrying). The one-way and two-way machines differ in exactly one place: for a
     * one-way RPC DELIVERED is a terminal success, so it is never an allowed predecessor of a terminal write.
     *
     * @return an immutable set; the same instance on every call
     */
    public Set<RpcStatus> getAllowedFromStatuses(RpcKind kind) {
        return (RpcKind.ONE_WAY == kind ? ALLOWED_FROM_ONE_WAY : ALLOWED_FROM_TWO_WAY).get(this);
    }

    private static Map<RpcStatus, Set<RpcStatus>> allowedFrom(RpcKind kind) {
        Map<RpcStatus, Set<RpcStatus>> byStatus = new EnumMap<>(RpcStatus.class);
        for (RpcStatus status : values()) {
            byStatus.put(status, Collections.unmodifiableSet(status.computeAllowedFrom(kind)));
        }
        return Collections.unmodifiableMap(byStatus);
    }

    private Set<RpcStatus> computeAllowedFrom(RpcKind kind) {
        return switch (this) {
            case SENT -> EnumSet.of(QUEUED, TIMEOUT);
            case DELIVERED -> EnumSet.of(QUEUED, SENT, TIMEOUT);
            case QUEUED -> EnumSet.of(SENT, TIMEOUT);                     // retry re-queue
            case TIMEOUT -> EnumSet.of(SENT);                            // delivery ack timed out while SENT
            // Terminals may overwrite any in-flight predecessor: async status writes and status-vs-response
            // message reordering can leave the row at QUEUED when the outcome lands. One-way DELIVERED is a
            // terminal success, so it is excluded for one-way; SUCCESSFUL is unreachable for one-way (no device
            // response) but stays consistent with FAILED/EXPIRED.
            case SUCCESSFUL, FAILED, EXPIRED -> RpcKind.ONE_WAY == kind
                    ? EnumSet.of(QUEUED, SENT, TIMEOUT)
                    : EnumSet.of(QUEUED, SENT, DELIVERED, TIMEOUT);
            case DELETED -> EnumSet.noneOf(RpcStatus.class);             // not written via the guarded UPDATE
        };
    }

}
