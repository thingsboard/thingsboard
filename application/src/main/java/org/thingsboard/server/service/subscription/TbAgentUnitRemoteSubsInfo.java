// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.subscription;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.log.LogStreamDispatcher;

public class TbAgentUnitRemoteSubsInfo extends TbEntityRemoteSubsInfo {

    public TbAgentUnitRemoteSubsInfo(TenantId tenantId, EntityId entityId) {
        super(tenantId, entityId);
    }

    @Override
    public TbEntitySubsUpdateInfo updateAndCheckIsEmpty(String serviceId, TbEntitySubEvent event) {
        boolean isEmptyLogSubsBeforeEvent = !coversLogs();
        TbEntitySubsUpdateInfo generalUpdInfo = super.updateAndCheckIsEmpty(serviceId, event);
        boolean isEmptyLogSubsAfterEvent = !coversLogs();

        return new TbAgentUnitSubsUpdateInfo(generalUpdInfo.isDuplicate(), generalUpdInfo.isEmpty(),
                isEmptyLogSubsBeforeEvent, isEmptyLogSubsAfterEvent);
    }

    @Override
    public TbEntitySubsUpdateInfo removeAndGetUpdateInfo(String serviceId) {
        boolean isEmptyLogSubsBeforeEvent = !coversLogs();
        TbEntitySubsUpdateInfo generalUpdInfo = super.removeAndGetUpdateInfo(serviceId);
        if (generalUpdInfo == null) {
            return null;
        }
        return new TbAgentUnitSubsUpdateInfo(false, generalUpdInfo.isEmpty(),
                isEmptyLogSubsBeforeEvent, !coversLogs());
    }

    public boolean coversLogs() {
        return getSubs().values().stream().anyMatch(TbSubscriptionsInfo::isLogs);
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static final class TbAgentUnitSubsUpdateInfo extends TbEntitySubsUpdateInfo {

        private boolean emptyLogSubsBeforeEvent;
        private boolean emptyLogSubsAfterEvent;

        public TbAgentUnitSubsUpdateInfo(boolean isDuplicate, boolean isEmpty,
                                         boolean emptyLogSubsBeforeEvent, boolean emptyLogSubsAfterEvent) {
            super(isDuplicate, isEmpty);
            this.emptyLogSubsBeforeEvent = emptyLogSubsBeforeEvent;
            this.emptyLogSubsAfterEvent = emptyLogSubsAfterEvent;
        }
    }
}
