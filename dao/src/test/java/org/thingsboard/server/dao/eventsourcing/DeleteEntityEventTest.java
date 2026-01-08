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