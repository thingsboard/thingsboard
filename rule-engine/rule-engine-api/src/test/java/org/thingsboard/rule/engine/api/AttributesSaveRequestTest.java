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
import org.thingsboard.common.util.NoOpFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;

class AttributesSaveRequestTest {

    @Test
    void testDefaultProcessingStrategyIsProcessAll() {
        var request = AttributesSaveRequest.builder().build();

        assertThat(request.getStrategy()).isEqualTo(AttributesSaveRequest.Strategy.PROCESS_ALL);
    }

    @Test
    void testNullProcessingStrategyIsProcessAll() {
        var request = AttributesSaveRequest.builder().strategy(null).build();

        assertThat(request.getStrategy()).isEqualTo(AttributesSaveRequest.Strategy.PROCESS_ALL);
    }

    @Test
    void testProcessAllStrategy() {
        assertThat(AttributesSaveRequest.Strategy.PROCESS_ALL).isEqualTo(new AttributesSaveRequest.Strategy(true, true, true));
    }

    @Test
    void testWsOnlyStrategy() {
        assertThat(AttributesSaveRequest.Strategy.WS_ONLY).isEqualTo(new AttributesSaveRequest.Strategy(false, true, false));
    }

    @Test
    void testSkipAllStrategy() {
        assertThat(AttributesSaveRequest.Strategy.SKIP_ALL).isEqualTo(new AttributesSaveRequest.Strategy(false, false, false));
    }

    @Test
    void testDefaultCallbackIsNoOp() {
        var request = AttributesSaveRequest.builder().build();

        assertThat(request.getCallback()).isEqualTo(NoOpFutureCallback.instance());
    }

    @Test
    void testNullCallbackIsNoOp() {
        var request = AttributesSaveRequest.builder().callback(null).build();

        assertThat(request.getCallback()).isEqualTo(NoOpFutureCallback.instance());
    }

}
