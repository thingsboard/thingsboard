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
package org.thingsboard.rule.engine.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleEngineDeviceRpcRequestTest {

    private static final long EXPIRATION_TIME = 1_700_000_000_000L;
    private static final long RESPONSE_DEADLINE = 1_700_000_015_000L;

    @Test
    public void givenResponseDeadlineIsNotSet_whenGetRuleEngineResponseDeadline_thenReturnsExpirationTime() {
        RuleEngineDeviceRpcRequest request = RuleEngineDeviceRpcRequest.builder()
                .expirationTime(EXPIRATION_TIME)
                .build();

        assertThat(request.getRuleEngineResponseDeadline()).isEqualTo(EXPIRATION_TIME);
    }

    @Test
    public void givenResponseDeadlineIsNotPositive_whenGetRuleEngineResponseDeadline_thenReturnsExpirationTime() {
        RuleEngineDeviceRpcRequest request = RuleEngineDeviceRpcRequest.builder()
                .expirationTime(EXPIRATION_TIME)
                .ruleEngineResponseDeadline(-1)
                .build();

        assertThat(request.getRuleEngineResponseDeadline()).isEqualTo(EXPIRATION_TIME);
    }

    @Test
    public void givenResponseDeadlineIsSet_whenGetRuleEngineResponseDeadline_thenReturnsResponseDeadline() {
        RuleEngineDeviceRpcRequest request = RuleEngineDeviceRpcRequest.builder()
                .expirationTime(EXPIRATION_TIME)
                .ruleEngineResponseDeadline(RESPONSE_DEADLINE)
                .build();

        assertThat(request.getRuleEngineResponseDeadline()).isEqualTo(RESPONSE_DEADLINE);
    }
}
