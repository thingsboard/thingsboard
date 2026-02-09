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
package org.thingsboard.server.common.msg.queue;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TopicPartitionInfoTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @Test
    public void givenTopicPartitionInfo_whenEquals_thenTrue() {

        TopicPartitionInfo tpiExpected = TopicPartitionInfo.builder()
                .topic("tb_core")
                .tenantId(null)
                .partition(4)
                .myPartition(true) //will ignored on equals
                .build();

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , is(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , is(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(tenantId)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , is(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(tenantId)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()));

    }

    @Test
    public void givenTopicPartitionInfo_whenEquals_thenFalse() {

        TopicPartitionInfo tpiExpected = TopicPartitionInfo.builder()
                .topic("tb_core")
                .tenantId(null)
                .partition(4)
                .myPartition(true) //will ignored on equals
                .build();

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(1)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(null)
                        .partition(1)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("js_eval")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("js_eval")
                        .tenantId(null)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(tenantId)
                        .partition(4)
                        .myPartition(true) //will ignored on equals
                        .build()
                , not(tpiExpected));

        assertThat(TopicPartitionInfo.builder()
                        .topic("tb_core")
                        .tenantId(tenantId)
                        .partition(4)
                        .myPartition(false) //will ignored on equals
                        .build()
                , not(tpiExpected));

    }
}