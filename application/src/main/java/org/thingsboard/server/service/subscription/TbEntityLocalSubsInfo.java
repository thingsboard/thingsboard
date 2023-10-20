package org.thingsboard.server.service.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.ws.notification.sub.NotificationsCountSubscription;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscription;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Information about the local websocket subscriptions.
 */
@RequiredArgsConstructor
public class TbEntityLocalSubsInfo {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityId entityId;
    @Getter
    private final Lock lock = new ReentrantLock();
    @Getter
    private final Set<TbSubscription<?>> subs = ConcurrentHashMap.newKeySet();

    // volatile every field or RW locks?
    private TbSubscriptionsInfo state = new TbSubscriptionsInfo();

    public TbEntitySubEvent add(TbSubscription<?> subscription) {
        subs.add(subscription);
        boolean stateChanged = false;
        if (subscription instanceof NotificationsSubscription || subscription instanceof NotificationsCountSubscription) {
            if (!state.notifications) {
                state.notifications = true;
                stateChanged = true;
            }
        } else if (subscription instanceof TbAlarmsSubscription) {
            if (!state.alarms) {
                state.alarms = true;
                stateChanged = true;
            }
        } else if (subscription instanceof TbTimeseriesSubscription) {
            var tsSub = (TbTimeseriesSubscription) subscription;
            if (!state.tsAllKeys) {
                if (tsSub.isAllKeys()) {
                    state.tsAllKeys = true;
                    stateChanged = true;
                } else {
                    if (state.tsKeys == null) {
                        state.tsKeys = new HashSet<>(tsSub.getKeyStates().keySet());
                        stateChanged = true;
                    } else if (state.tsKeys.addAll(tsSub.getKeyStates().keySet())) {
                        stateChanged = true;
                    }
                }
            }
        } else if (subscription instanceof TbAttributeSubscription) {
            var attrSub = (TbAttributeSubscription) subscription;
            if (!state.attrAllKeys) {
                if (attrSub.isAllKeys()) {
                    state.attrAllKeys = true;
                    stateChanged = true;
                } else {
                    if (state.attrKeys == null) {
                        state.attrKeys = new HashSet<>(attrSub.getKeyStates().keySet());
                        stateChanged = true;
                    } else if (state.attrKeys.addAll(attrSub.getKeyStates().keySet())) {
                        stateChanged = true;
                    }
                }
            }
        }
        return stateChanged ? toEvent(ComponentLifecycleEvent.UPDATED) : null;
    }

    public TbEntitySubEvent remove(TbSubscription<?> toRemove) {
        if (!subs.remove(toRemove)) {
            return null;
        }
        if (subs.isEmpty()) {
            return TbEntitySubEvent.builder().entityId(entityId).type(ComponentLifecycleEvent.DELETED).build();
        }
        TbSubscriptionsInfo oldState = state.copy();
        TbSubscriptionsInfo newState = new TbSubscriptionsInfo();
        for (TbSubscription<?> subscription : subs) {
            if (subscription instanceof NotificationsSubscription || subscription instanceof NotificationsCountSubscription) {
                if (!newState.notifications) {
                    newState.notifications = true;
                }
            } else if (subscription instanceof TbAlarmsSubscription) {
                if (!newState.alarms) {
                    newState.alarms = true;
                }
            } else if (subscription instanceof TbTimeseriesSubscription) {
                var tsSub = (TbTimeseriesSubscription) subscription;
                if (!newState.tsAllKeys && tsSub.isAllKeys()) {
                    newState.tsAllKeys = true;
                    continue;
                }
                if (newState.tsKeys == null) {
                    newState.tsKeys = new HashSet<>(tsSub.getKeyStates().keySet());
                } else {
                    newState.tsKeys.addAll(tsSub.getKeyStates().keySet());
                }
            } else if (subscription instanceof TbAttributeSubscription) {
                var attrSub = (TbAttributeSubscription) subscription;
                if (!newState.attrAllKeys && attrSub.isAllKeys()) {
                    newState.attrAllKeys = true;
                    continue;
                }
                if (newState.attrKeys == null) {
                    newState.attrKeys = new HashSet<>(attrSub.getKeyStates().keySet());
                } else {
                    newState.attrKeys.addAll(attrSub.getKeyStates().keySet());
                }
            }
        }
        if (newState.equals(oldState)) {
            return null;
        } else {
            this.state = newState;
            return toEvent(ComponentLifecycleEvent.UPDATED);
        }
    }

    public TbEntitySubEvent toEvent(ComponentLifecycleEvent type) {
        var result = TbEntitySubEvent.builder().entityId(entityId).type(type);
        if (!ComponentLifecycleEvent.DELETED.equals(type)) {
            result.info(state.copy());
        }
        return result.build();
    }

    public boolean isNf() {
        return state.notifications;
    }
}
