/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.queue.discovery;

import lombok.AllArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceQueue;

import java.util.Objects;

@AllArgsConstructor
public class TopicPartitionInfoKey {
    private ServiceQueue serviceQueue;
    private TenantId isolatedTenantId;
    private int partition;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicPartitionInfoKey that = (TopicPartitionInfoKey) o;
        return partition == that.partition &&
                serviceQueue.equals(that.serviceQueue) &&
                Objects.equals(isolatedTenantId, that.isolatedTenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceQueue, isolatedTenantId, partition);
    }
}
