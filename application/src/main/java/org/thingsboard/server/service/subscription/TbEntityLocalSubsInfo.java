package org.thingsboard.server.service.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.ws.notification.sub.NotificationsCountSubscription;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscription;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Information about the local websocket subscriptions.
 */
@RequiredArgsConstructor
public class TbEntityLocalSubsInfo extends TbSubscriptionsInfo {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityId entityId;
    @Getter
    private final Lock lock = new ReentrantLock();
    @Getter
    private final Set<TbSubscription> subs = ConcurrentHashMap.newKeySet();

    public TbEntitySubEvent add(TbSubscription subscription) {
        subs.add(subscription);
        if (subscription instanceof NotificationsSubscription || subscription instanceof NotificationsCountSubscription) {
            if (!notifications) {
                notifications = true;
                return newEvent().notifications(true).build();
            }
        } else if (subscription instanceof TbAlarmsSubscription) {
            if (!alarms) {
                alarms = true;
                return newEvent().alarms(true).build();
            }
        } else if (subscription instanceof TbTimeseriesSubscription) {
            var tsSub = (TbTimeseriesSubscription) subscription;
            if (!tsAllKeys && tsSub.isAllKeys()) {
                tsAllKeys = true;
                return newEvent().tsAllKeys(true).build();
            }
            if (tsKeys == null) {
                tsKeys = new HashSet<>(tsSub.getKeyStates().keySet());
                return newEvent().tsKeys(tsKeys).build();
            } else if (tsKeys.addAll(tsSub.getKeyStates().keySet())) {
                //TODO: We may want to send only new keys.
                return newEvent().tsKeys(tsKeys).build();
            }
        } else if (subscription instanceof TbAttributeSubscription) {
            var attrSub = (TbAttributeSubscription) subscription;
            if (!attrAllKeys && attrSub.isAllKeys()) {
                attrAllKeys = true;
                return newEvent().attrAllKeys(true).build();
            }
            if (attrKeys == null) {
                attrKeys = new HashSet<>(attrSub.getKeyStates().keySet());
                return newEvent().attrKeys(attrKeys).build();
            } else if (attrKeys.addAll(attrSub.getKeyStates().keySet())) {
                //TODO: We may want to send only new keys.
                return newEvent().attrKeys(attrKeys).build();
            }
        }
        return null;
    }

    private TbEntitySubEvent.TbEntitySubEventBuilder newEvent() {
        return TbEntitySubEvent.builder().entityId(entityId).type(subs.isEmpty() ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
    }
}
