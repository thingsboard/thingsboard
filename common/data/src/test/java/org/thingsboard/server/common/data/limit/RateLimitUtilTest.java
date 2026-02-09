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
package org.thingsboard.server.common.data.limit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitUtilTest {

    @Test
    @DisplayName("LimitedApiUtil should parse single entry correctly")
    void testParseSingleEntry() {
        List<RateLimitEntry> entries = RateLimitUtil.parseConfig("100:60");

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).capacity()).isEqualTo(100);
        assertThat(entries.get(0).durationSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("LimitedApiUtil should parse multiple entries correctly")
    void testParseMultipleEntries() {
        List<RateLimitEntry> entries = RateLimitUtil.parseConfig("100:60,200:30");

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).capacity()).isEqualTo(100);
        assertThat(entries.get(0).durationSeconds()).isEqualTo(60);
        assertThat(entries.get(1).capacity()).isEqualTo(200);
        assertThat(entries.get(1).durationSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("LimitedApiUtil should return empty list for null or empty config")
    void testParseEmptyConfig() {
        assertThat(RateLimitUtil.parseConfig(null)).isEmpty();
        assertThat(RateLimitUtil.parseConfig("")).isEmpty();
    }

    @Test
    @DisplayName("LimitedApiUtil should merge two configs by summing capacities with same durations")
    void testMergeStrConfigs() {
        Function<DefaultTenantProfileConfiguration, String> extractor1 = cfg -> "100:60,50:30";
        Function<DefaultTenantProfileConfiguration, String> extractor2 = cfg -> "200:60,25:10";

        // Fake config instance (not used directly in lambda logic)
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();

        String result = RateLimitUtil.merge(extractor1, extractor2).apply(config);

        // Should be: 300:60 (100+200), 50:30, 25:10
        assertThat(result).isEqualTo("25:10,50:30,300:60");
    }

    @Test
    @DisplayName("LimitedApiUtil should merge configs when one is empty")
    void testMergeWithEmptyOne() {
        Function<DefaultTenantProfileConfiguration, String> extractor1 = cfg -> "100:60";
        Function<DefaultTenantProfileConfiguration, String> extractor2 = cfg -> "";

        // Fake config instance (not used directly in lambda logic)
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();
        String result = RateLimitUtil.merge(extractor1, extractor2).apply(config);

        assertThat(result).isEqualTo("100:60");
    }

    @Test
    @DisplayName("LimitedApiUtil should merge configs when both have distinct durations")
    void testMergeWithDistinctDurations() {
        Function<DefaultTenantProfileConfiguration, String> extractor1 = cfg -> "100:60";
        Function<DefaultTenantProfileConfiguration, String> extractor2 = cfg -> "200:10";

        // Fake config instance (not used directly in lambda logic)
        DefaultTenantProfileConfiguration config = new DefaultTenantProfileConfiguration();
        String result = RateLimitUtil.merge(extractor1, extractor2).apply(config);

        assertThat(result).isEqualTo("200:10,100:60");
    }

}
