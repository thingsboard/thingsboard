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
package org.thingsboard.server.common.msg.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TbRateLimitsTest {

    @Test
    @DisplayName("TbRateLimits should construct with single rate limit")
    void testSingleLimitConstructor() {
        TbRateLimits limits = new TbRateLimits("10:1", false);
        assertThat(limits.getConfiguration()).isEqualTo("10:1");
    }

    @Test
    @DisplayName("TbRateLimits should construct with multiple rate limits")
    void testMultipleLimitConstructor() {
        String config = "10:1,100:10";
        TbRateLimits limits = new TbRateLimits(config, false);
        assertThat(limits.getConfiguration()).isEqualTo(config);
    }

    @Test
    @DisplayName("TbRateLimits should throw IllegalArgumentException on empty string")
    void testEmptyConfigThrows() {
        assertThatThrownBy(() -> new TbRateLimits("", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse rate limits configuration: ");
    }

    @Test
    @DisplayName("TbRateLimits should throw NumberFormatException on malformed value")
    void testMalformedConfigThrows() {
        assertThatThrownBy(() -> new TbRateLimits("not_a_number:second", false))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("TbRateLimits should throw ArrayIndexOutOfBoundsException on missing colon")
    void testColonMissingThrows() {
        assertThatThrownBy(() -> new TbRateLimits("100", false))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

}