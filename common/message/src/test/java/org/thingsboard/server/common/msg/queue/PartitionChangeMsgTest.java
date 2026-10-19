// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.msg.queue;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PartitionChangeMsgTest {

    private final TenantId tenant1 = TenantId.fromUUID(UUID.randomUUID());
    private final TenantId tenant2 = TenantId.fromUUID(UUID.randomUUID());

    @Test
    public void forAllTenants_affectsEveryTenant() {
        PartitionChangeMsg msg = PartitionChangeMsg.forAllTenants(ServiceType.TB_RULE_ENGINE);

        assertThat(msg.getServiceType()).isEqualTo(ServiceType.TB_RULE_ENGINE);
        assertThat(msg.getMsgType()).isEqualTo(MsgType.PARTITION_CHANGE_MSG);
        assertThat(msg.isForAllTenants()).isTrue();
        assertThat(msg.isAffected(tenant1)).isTrue();
        assertThat(msg.isAffected(tenant2)).isTrue();
    }

    @Test
    public void forTenants_affectsOnlyGivenTenants() {
        Set<TenantId> tenants = new HashSet<>(Set.of(tenant1));
        PartitionChangeMsg msg = PartitionChangeMsg.forTenants(ServiceType.TB_RULE_ENGINE, tenants);
        tenants.add(tenant2);

        assertThat(msg.isForAllTenants()).isFalse();
        assertThat(msg.isAffected(tenant1)).isTrue();
        assertThat(msg.isAffected(tenant2)).isFalse();
    }

    @Test
    public void toString_doesNotListAffectedTenants() {
        PartitionChangeMsg msg = PartitionChangeMsg.forTenants(ServiceType.TB_RULE_ENGINE, Set.of(tenant1));

        assertThat(msg.toString()).doesNotContain(tenant1.getId().toString());
    }

}
