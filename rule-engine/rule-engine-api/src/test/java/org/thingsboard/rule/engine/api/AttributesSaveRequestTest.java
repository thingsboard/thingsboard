// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
