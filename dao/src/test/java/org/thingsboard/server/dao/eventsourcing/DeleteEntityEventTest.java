// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.eventsourcing;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.byLessThan;

class DeleteEntityEventTest {

    @Test
    void testBuilderDefaultTs() {
        assertThat(DeleteEntityEvent.builder().build().getTs())
                .isCloseTo(System.currentTimeMillis(), byLessThan(TimeUnit.MINUTES.toMillis(1)));

        assertThat(DeleteEntityEvent.builder().ts(Long.MIN_VALUE).build().getTs())
                .isEqualTo(Long.MIN_VALUE);
        assertThat(DeleteEntityEvent.builder().ts(Long.MAX_VALUE).build().getTs())
                .isEqualTo(Long.MAX_VALUE);
        assertThat(DeleteEntityEvent.builder().ts(-1L).build().getTs())
                .isEqualTo(-1L);
        assertThat(DeleteEntityEvent.builder().ts(0L).build().getTs())
                .isEqualTo(0L);

        assertThat(DeleteEntityEvent.builder().ts(1692175215000L).build().getTs())
                .isEqualTo(1692175215000L);
    }

}