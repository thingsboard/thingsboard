// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.subscription;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.subscription.TbEntityRemoteSubsInfo.TbEntitySubsUpdateInfo;

import java.util.function.Supplier;

public interface SubEventObserver {

    EntityType entityType();

    void onSubEvent(TbEntitySubEvent event, TbEntitySubsUpdateInfo entityUpdateInfo,
                    Supplier<TbEntityRemoteSubsInfo> committedSubsInfo);

    void onSubsRemoved(TenantId tenantId, EntityId entityId, TbEntitySubsUpdateInfo entityUpdateInfo,
                       Supplier<TbEntityRemoteSubsInfo> committedSubsInfo);

    default TbEntityRemoteSubsInfo createSubsInfo(TenantId tenantId, EntityId entityId) {
        return new TbEntityRemoteSubsInfo(tenantId, entityId);
    }

}
