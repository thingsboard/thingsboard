// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.rpc;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RpcSubmitStrategyTest {

    @Test
    void givenRandomString_whenParse_thenReturnBurstStrategy() {
        String randomString = StringUtils.randomAlphanumeric(10);
        RpcSubmitStrategy parsed = RpcSubmitStrategy.parse(randomString);
        assertThat(parsed).isEqualTo(RpcSubmitStrategy.BURST);
    }

    @Test
    void givenNull_whenParse_thenReturnBurstStrategy() {
        RpcSubmitStrategy parsed = RpcSubmitStrategy.parse(null);
        assertThat(parsed).isEqualTo(RpcSubmitStrategy.BURST);
    }

}
