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

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.Set;

/**
 * @author Andrew Shvayka
 */
@Data
public final class PartitionChangeMsg implements TbActorMsg {

    private final ServiceType serviceType;

    /**
     * Tenants whose partition ownership changed. Null means the shared
     * (non-isolated) tenant pool itself changed, so every tenant actor must
     * re-verify — shared-pool tenants aren't individually tracked as
     * queueKeys, so they can't be enumerated here.
     */
    private final Set<TenantId> affectedTenants;

    public PartitionChangeMsg(ServiceType serviceType) {
        this(serviceType, null);
    }

    public PartitionChangeMsg(ServiceType serviceType, Set<TenantId> affectedTenants) {
        this.serviceType = serviceType;
        this.affectedTenants = affectedTenants;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.PARTITION_CHANGE_MSG;
    }
}
