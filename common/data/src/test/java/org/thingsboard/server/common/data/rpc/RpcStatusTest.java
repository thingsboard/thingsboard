// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.rpc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.rpc.RpcStatus.DELIVERED;
import static org.thingsboard.server.common.data.rpc.RpcStatus.QUEUED;
import static org.thingsboard.server.common.data.rpc.RpcStatus.SENT;

class RpcStatusTest {

    private static final List<RpcStatus> intermediateStatuses = List.of(
            QUEUED,
            SENT,
            DELIVERED
    );

    @Test
    void isIntermediateStatusTest() {
        var rpcStatuses = RpcStatus.values();
        for (var status : rpcStatuses) {
            if (intermediateStatuses.contains(status)) {
                assertThat(status.isIntermediate()).isTrue();
            } else {
                assertThat(status.isIntermediate()).isFalse();
            }
        }
    }

}
