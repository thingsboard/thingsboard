// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.rpc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        assertThat(SENT.getAllowedFromStatuses(RpcKind.TWO_WAY)).containsExactlyInAnyOrder(QUEUED, TIMEOUT);
        assertThat(DELIVERED.getAllowedFromStatuses(RpcKind.TWO_WAY)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        assertThat(QUEUED.getAllowedFromStatuses(RpcKind.TWO_WAY)).containsExactlyInAnyOrder(SENT, TIMEOUT);
        assertThat(TIMEOUT.getAllowedFromStatuses(RpcKind.TWO_WAY)).containsExactlyInAnyOrder(SENT);
        assertThat(SUCCESSFUL.getAllowedFromStatuses(RpcKind.TWO_WAY)).containsExactlyInAnyOrder(QUEUED, SENT, DELIVERED, TIMEOUT);
        assertThat(FAILED.getAllowedFromStatuses(RpcKind.TWO_WAY)).containsExactlyInAnyOrder(QUEUED, SENT, DELIVERED, TIMEOUT);
        assertThat(EXPIRED.getAllowedFromStatuses(RpcKind.TWO_WAY)).containsExactlyInAnyOrder(QUEUED, SENT, DELIVERED, TIMEOUT);
        assertThat(DELETED.getAllowedFromStatuses(RpcKind.TWO_WAY)).isEmpty();
    }

    @Test
    void allowedFromStatuses_oneWayExcludesDeliveredForTerminals() {
        assertThat(SUCCESSFUL.getAllowedFromStatuses(RpcKind.ONE_WAY)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        assertThat(FAILED.getAllowedFromStatuses(RpcKind.ONE_WAY)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        assertThat(EXPIRED.getAllowedFromStatuses(RpcKind.ONE_WAY)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        // non-terminal targets ignore oneway
        assertThat(DELIVERED.getAllowedFromStatuses(RpcKind.ONE_WAY)).containsExactlyInAnyOrder(QUEUED, SENT, TIMEOUT);
        assertThat(SENT.getAllowedFromStatuses(RpcKind.ONE_WAY)).containsExactlyInAnyOrder(QUEUED, TIMEOUT);
    }

    @Test
    void terminalStatusesNeverAppearAsAllowedFromSource() {
        // A terminal status must never be overwritable by any transition -> it appears in no allowed-from set.
        Set<RpcStatus> terminals = Set.of(SUCCESSFUL, FAILED, EXPIRED, DELETED);
        for (RpcStatus target : RpcStatus.values()) {
            assertThat(target.getAllowedFromStatuses(RpcKind.TWO_WAY))
                    .as("target %s (two-way) must not allow overwriting a terminal status", target)
                    .doesNotContainAnyElementsOf(terminals);
            assertThat(target.getAllowedFromStatuses(RpcKind.ONE_WAY))
                    .as("target %s (one-way) must not allow overwriting a terminal status", target)
                    .doesNotContainAnyElementsOf(terminals);
        }
    }

    @Test
    void allowedFromStatusesAreSharedAndImmutable() {
        // The sets are precomputed once and handed to every caller, so a mutable set here would let one caller
        // corrupt the state machine for all of them.
        for (RpcKind kind : RpcKind.values()) {
            for (RpcStatus target : RpcStatus.values()) {
                Set<RpcStatus> allowed = target.getAllowedFromStatuses(kind);
                assertThat(target.getAllowedFromStatuses(kind))
                        .as("%s (%s) must hand out the same instance on every call", target, kind)
                        .isSameAs(allowed);
                assertThatThrownBy(() -> allowed.add(QUEUED))
                        .as("%s (%s) must hand out an unmodifiable set", target, kind)
                        .isInstanceOf(UnsupportedOperationException.class);
            }
        }
    }

}
