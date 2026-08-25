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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.rpc.RpcStatus.DELIVERED;
import static org.thingsboard.server.common.data.rpc.RpcStatus.QUEUED;
import static org.thingsboard.server.common.data.rpc.RpcStatus.SENT;
import static org.thingsboard.server.common.data.rpc.RpcStatus.EXPIRED;
import static org.thingsboard.server.common.data.rpc.RpcStatus.FAILED;
import static org.thingsboard.server.common.data.rpc.RpcStatus.SUCCESSFUL;
import static org.thingsboard.server.common.data.rpc.RpcStatus.DELETED;
import static org.thingsboard.server.common.data.rpc.RpcStatus.TIMEOUT;

class RpcStatusTest {

    private static final List<RpcStatus> pushDeleteNotificationToCoreStatuses = List.of(
            QUEUED,
            SENT,
            DELIVERED
    );

    @Test
    void isPushDeleteNotificationToCoreStatusTest() {
        var rpcStatuses = RpcStatus.values();
        for (var status : rpcStatuses) {
            if (pushDeleteNotificationToCoreStatuses.contains(status)) {
                assertThat(status.isPushDeleteNotificationToCore()).isTrue();
            } else {
                assertThat(status.isPushDeleteNotificationToCore()).isFalse();
            }
        }
    }

    @Test
    void allowedFromStatuses_twoWay() {
        assertThat(SENT.getAllowedFromStatuses(false)).containsExactlyInAnyOrder(QUEUED, TIMEOUT);
        assertThat(DELIVERED.getAllowedFromStatuses(false)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        assertThat(QUEUED.getAllowedFromStatuses(false)).containsExactlyInAnyOrder(SENT, TIMEOUT);
        assertThat(TIMEOUT.getAllowedFromStatuses(false)).containsExactlyInAnyOrder(SENT);
        assertThat(SUCCESSFUL.getAllowedFromStatuses(false)).containsExactlyInAnyOrder(QUEUED, SENT, DELIVERED, TIMEOUT);
        assertThat(FAILED.getAllowedFromStatuses(false)).containsExactlyInAnyOrder(QUEUED, SENT, DELIVERED, TIMEOUT);
        assertThat(EXPIRED.getAllowedFromStatuses(false)).containsExactlyInAnyOrder(QUEUED, SENT, DELIVERED, TIMEOUT);
        assertThat(DELETED.getAllowedFromStatuses(false)).isEmpty();
    }

    @Test
    void allowedFromStatuses_oneWayExcludesDeliveredForTerminals() {
        assertThat(SUCCESSFUL.getAllowedFromStatuses(true)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        assertThat(FAILED.getAllowedFromStatuses(true)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        assertThat(EXPIRED.getAllowedFromStatuses(true)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        // non-terminal targets ignore oneway
        assertThat(DELIVERED.getAllowedFromStatuses(true)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        assertThat(SENT.getAllowedFromStatuses(true)).containsExactlyInAnyOrder(QUEUED, TIMEOUT);
    }

    @Test
    void terminalStatusesNeverAppearAsAllowedFromSource() {
        // A terminal status must never be overwritable by any transition -> it appears in no allowed-from set.
        Set<RpcStatus> terminals = Set.of(SUCCESSFUL, FAILED, EXPIRED, DELETED);
        for (RpcStatus target : RpcStatus.values()) {
            assertThat(target.getAllowedFromStatuses(false))
                    .as("target %s (two-way) must not allow overwriting a terminal status", target)
                    .doesNotContainAnyElementsOf(terminals);
            assertThat(target.getAllowedFromStatuses(true))
                    .as("target %s (one-way) must not allow overwriting a terminal status", target)
                    .doesNotContainAnyElementsOf(terminals);
        }
    }

}
