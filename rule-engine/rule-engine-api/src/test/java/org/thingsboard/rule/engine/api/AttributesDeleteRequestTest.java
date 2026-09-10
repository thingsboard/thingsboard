// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.NoOpFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;

class AttributesDeleteRequestTest {

    @Test
    void testDefaultCallbackIsNoOp() {
        var request = AttributesDeleteRequest.builder().build();

        assertThat(request.getCallback()).isEqualTo(NoOpFutureCallback.instance());
    }

    @Test
    void testNullCallbackIsNoOp() {
        var request = AttributesDeleteRequest.builder().callback(null).build();

        assertThat(request.getCallback()).isEqualTo(NoOpFutureCallback.instance());
    }

}
