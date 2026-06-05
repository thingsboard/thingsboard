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
package org.thingsboard.server.common.transport;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

public final class TenantDeletedEvent extends TbApplicationEvent {

    private static final long serialVersionUID = -3801673609913423334L;
    @Getter
    private final TenantId tenantId;

    public TenantDeletedEvent(TenantId tenantId) {
        super(new Object());
        this.tenantId = tenantId;
    }
}
