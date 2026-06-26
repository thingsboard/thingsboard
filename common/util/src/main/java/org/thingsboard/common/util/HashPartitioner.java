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
package org.thingsboard.common.util;

/**
 * Maps a hash code to a non-negative partition index in {@code [0, partitions)}.
 * <p>
 * Centralises the {@code (hashCode & 0x7FFFFFFF) % partitions} striping so that components which must
 * agree on a partition for the same key derive it identically. In particular, batched RPC persistence
 * relies on this: {@code TbSqlBlockingQueueWrapper} picks the DB write partition and {@code TbRpcService}
 * picks the post-persist callback stripe, both keyed off the rpcId hash. If those two ever disagreed on
 * how the index is computed, the submission-order guarantee (e.g. RPC_QUEUED before RPC_DELIVERED) would
 * silently break — keeping the math in one place removes that risk.
 */
public final class HashPartitioner {

    private HashPartitioner() {
    }

    /**
     * @param hashCode  hash of the partition key
     * @param partitions number of partitions; must be {@code > 0}
     * @return a non-negative partition index in {@code [0, partitions)}
     */
    public static int resolvePartition(int hashCode, int partitions) {
        return (hashCode & 0x7FFFFFFF) % partitions;
    }
}
