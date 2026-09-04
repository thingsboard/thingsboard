// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

import lombok.Getter;
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

    public boolean updateAndCheckIsEmpty(String serviceId, TbEntitySubEvent event) {
        var current = subs.get(serviceId);
        if (current != null && current.seqNumber > event.getSeqNumber()) {
            log.warn("[{}][{}] Duplicate subscription event received. Current: {}, Event: {}",
                    tenantId, entityId, current, event.getInfo());
            return false;
        }
        switch (event.getType()) {
            case CREATED:
                subs.put(serviceId, event.getInfo());
                break;
            case UPDATED:
                var newSubInfo = event.getInfo();
                if (newSubInfo.isEmpty()) {
                    subs.remove(serviceId);
                    return isEmpty();
                } else {
                    subs.put(serviceId, newSubInfo);
                }
                break;
            case DELETED:
                subs.remove(serviceId);
                return isEmpty();
        }
        return false;
    }

    public boolean removeAndCheckIsEmpty(String serviceId) {
        if (subs.remove(serviceId) != null) {
            return subs.isEmpty();
        } else {
            return false;
        }
    }

    public boolean isEmpty() {
        return subs.isEmpty();
    }
}
