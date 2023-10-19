package org.thingsboard.server.service.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information about the local websocket subscriptions.
 */
@RequiredArgsConstructor
public class TbEntityRemoteSubsInfo {
    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityId entityId;
    @Getter
    private final Map<String, TbSubscriptionsInfo> subs = new ConcurrentHashMap<>(); // By service ID

    public boolean updateAndCheckIsEmpty(String serviceId, TbEntitySubEvent event) {
        switch (event.getType()) {
            case CREATED:
                var newSubsInfo = new TbSubscriptionsInfo();
                newSubsInfo.update(event);
                subs.put(serviceId, newSubsInfo);
                break;
            case UPDATED:
                var oldSubsInfo = subs.computeIfAbsent(serviceId, id -> new TbSubscriptionsInfo());
                oldSubsInfo.update(event);
                if (oldSubsInfo.isEmpty()) {
                    subs.remove(serviceId);
                    return isEmpty();
                }
                break;
            case DELETED:
                subs.remove(serviceId);
                return isEmpty();
        }
        return false;
    }

    public boolean isEmpty() {
        return subs.isEmpty();
    }
}
