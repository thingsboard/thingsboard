// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
