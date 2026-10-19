// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.queue;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
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

    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    private final Set<TenantId> affectedTenants;

    private PartitionChangeMsg(ServiceType serviceType, Set<TenantId> affectedTenants) {
        this.serviceType = serviceType;
        this.affectedTenants = affectedTenants;
    }

    public static PartitionChangeMsg forAllTenants(ServiceType serviceType) {
        return new PartitionChangeMsg(serviceType, null);
    }

    public static PartitionChangeMsg forTenants(ServiceType serviceType, Set<TenantId> affectedTenants) {
        return new PartitionChangeMsg(serviceType, Set.copyOf(affectedTenants));
    }

    /**
     * @return {@code true} if every tenant actor must receive this message
     */
    public boolean isForAllTenants() {
        return affectedTenants == null;
    }

    public boolean isAffected(TenantId tenantId) {
        return affectedTenants == null || affectedTenants.contains(tenantId);
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.PARTITION_CHANGE_MSG;
    }
}
