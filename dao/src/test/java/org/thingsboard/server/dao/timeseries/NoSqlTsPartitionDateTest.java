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
package org.thingsboard.server.dao.timeseries;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class NoSqlTsPartitionDateTest {

    @ParameterizedTest
    @EnumSource(NoSqlTsPartitionDate.class)
    void getDurationMsTest(NoSqlTsPartitionDate tsPartitionDate) throws Exception {
        final Long durationMs = switch (tsPartitionDate) {
            case MINUTES -> 60000L;
            case HOURS -> 3600000L;
            case DAYS -> 86400000L;
            case MONTHS -> 2629746000L;
            case YEARS -> 31556952000L;
            case INDEFINITE -> Long.MAX_VALUE;
            default -> null; //should be here in case a new enum value will be added in future
        };
        assertThat(durationMs).isNotNull();
        assertThat(tsPartitionDate.getDurationMs()).isEqualTo(durationMs);
    }

}
