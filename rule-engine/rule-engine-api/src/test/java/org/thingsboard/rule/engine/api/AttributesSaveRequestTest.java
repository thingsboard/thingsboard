/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

class AttributesSaveRequestTest {

    @Test
    void testDefaultSaveStrategyIsProcessAll() {
        var request = AttributesSaveRequest.builder().build();

        assertThat(request.getStrategy()).isEqualTo(AttributesSaveRequest.Strategy.PROCESS_ALL);
    }

    @Test
    void testProcessAllStrategy() {
        assertThat(AttributesSaveRequest.Strategy.PROCESS_ALL).isEqualTo(new AttributesSaveRequest.Strategy(true, true));
    }

    @Test
    void testWsOnlyStrategy() {
        assertThat(AttributesSaveRequest.Strategy.WS_ONLY).isEqualTo(new AttributesSaveRequest.Strategy(false, true));
    }

    @Test
    void testSkipAllStrategy() {
        assertThat(AttributesSaveRequest.Strategy.SKIP_ALL).isEqualTo(new AttributesSaveRequest.Strategy(false, false));
    }

}
