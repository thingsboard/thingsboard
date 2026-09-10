// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.rpc;

import java.util.Arrays;

public enum RpcSubmitStrategy {

    BURST, SEQUENTIAL_ON_ACK_FROM_DEVICE, SEQUENTIAL_ON_RESPONSE_FROM_DEVICE;

    public static RpcSubmitStrategy parse(String strategyStr) {
        return Arrays.stream(RpcSubmitStrategy.values())
                .filter(strategy -> strategy.name().equalsIgnoreCase(strategyStr))
                .findFirst()
                .orElse(BURST);
    }
}
