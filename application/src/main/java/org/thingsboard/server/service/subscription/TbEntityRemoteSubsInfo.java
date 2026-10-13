// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information about the local websocket subscriptions.
 */
@RequiredArgsConstructor
@Slf4j
public class TbEntityRemoteSubsInfo {
    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityId entityId;
    @Getter
    private final Map<String, TbSubscriptionsInfo> subs = new ConcurrentHashMap<>(); // By service ID

    public TbEntitySubsUpdateInfo updateAndCheckIsEmpty(String serviceId, TbEntitySubEvent event) {
        var current = subs.get(serviceId);
        if (current != null && current.seqNumber > event.getSeqNumber()) {
            log.warn("[{}][{}] Duplicate subscription event received. Current: {}, Event: {}",
                    tenantId, entityId, current, event.getInfo());
            boolean isDuplicate = true;
            return new TbEntitySubsUpdateInfo(isDuplicate, isEmpty());
        }
        switch (event.getType()) {
            case CREATED:
                subs.put(serviceId, event.getInfo());
                break;
            case UPDATED:
                var newSubInfo = event.getInfo();
                if (newSubInfo.isEmpty()) {
                    subs.remove(serviceId);
                } else {
                    subs.put(serviceId, newSubInfo);
                }
                break;
            case DELETED:
                subs.remove(serviceId);
                break;
        }
        boolean isDuplicate = false;
        return new TbEntitySubsUpdateInfo(isDuplicate, isEmpty());
    }

    public TbEntitySubsUpdateInfo removeAndGetUpdateInfo(String serviceId) {
        if (subs.remove(serviceId) == null) {
            return null;
        }
        return new TbEntitySubsUpdateInfo(false, isEmpty());
    }

    public boolean isEmpty() {
        return subs.isEmpty();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TbEntitySubsUpdateInfo {

        private boolean isDuplicate;
        private boolean isEmpty;
    }
}
