// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
