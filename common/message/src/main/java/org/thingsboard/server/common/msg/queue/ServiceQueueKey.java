/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Objects;

@ToString
public class ServiceQueueKey {
    @Getter
    private final ServiceQueue serviceQueue;

    @Getter
    private final TenantId tenantId;

    public ServiceQueueKey(ServiceQueue serviceQueue, TenantId tenantId) {
        this.serviceQueue = serviceQueue;
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceQueueKey that = (ServiceQueueKey) o;
        return serviceQueue.equals(that.serviceQueue) &&
                Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceQueue, tenantId);
    }

    public ServiceType getServiceType() {
        return serviceQueue.getType();
    }
}
