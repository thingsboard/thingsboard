/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.subscription;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    // TODO: add sequence number to check that we have no race conditions in events, etc.
    // private final AtomicInteger seqNumber
    // volatile every field or RW locks?
    private volatile TbSubscriptionsInfo state = new TbSubscriptionsInfo();

    public TbEntitySubEvent add(TbSubscription<?> subscription) {
        log.trace("[{}][{}][{}] Adding: {}", tenantId, entityId, subscription.getSubscriptionId(), subscription);
        boolean created = subs.isEmpty();
        subs.add(subscription);
        TbSubscriptionsInfo newState = created ? state : state.copy();
        boolean stateChanged = false;
        if (subscription instanceof NotificationsSubscription || subscription instanceof NotificationsCountSubscription) {
            if (!newState.notifications) {
                newState.notifications = true;
                stateChanged = true;
            }
        } else if (subscription instanceof TbAlarmsSubscription) {
            if (!newState.alarms) {
                newState.alarms = true;
                stateChanged = true;
            }
        } else if (subscription instanceof TbTimeseriesSubscription) {
            var tsSub = (TbTimeseriesSubscription) subscription;
            if (!newState.tsAllKeys) {
                if (tsSub.isAllKeys()) {
                    newState.tsAllKeys = true;
                    stateChanged = true;
                } else {
                    if (newState.tsKeys == null) {
                        newState.tsKeys = new HashSet<>(tsSub.getKeyStates().keySet());
                        stateChanged = true;
                    } else if (newState.tsKeys.addAll(tsSub.getKeyStates().keySet())) {
                        stateChanged = true;
                    }
                }
            }
        } else if (subscription instanceof TbAttributeSubscription) {
            var attrSub = (TbAttributeSubscription) subscription;
            if (!newState.attrAllKeys) {
                if (attrSub.isAllKeys()) {
                    newState.attrAllKeys = true;
                    stateChanged = true;
                } else {
                    if (newState.attrKeys == null) {
                        newState.attrKeys = new HashSet<>(attrSub.getKeyStates().keySet());
                        stateChanged = true;
                    } else if (newState.attrKeys.addAll(attrSub.getKeyStates().keySet())) {
                        stateChanged = true;
                    }
                }
            }
        }
        if (stateChanged) {
            state = newState;
        }
        if (created) {
            return toEvent(ComponentLifecycleEvent.CREATED);
        } else if (stateChanged) {
            return toEvent(ComponentLifecycleEvent.UPDATED);
        } else {
            return null;
        }
    }

    public TbEntitySubEvent remove(TbSubscription<?> sub) {
        log.trace("[{}][{}][{}] Removing: {}", tenantId, entityId, sub.getSubscriptionId(), sub);
        if (!subs.remove(sub)) {
            return null;
        }
        if (subs.isEmpty()) {
            return TbEntitySubEvent.builder().tenantId(tenantId).entityId(entityId).type(ComponentLifecycleEvent.DELETED).build();
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
        var result = TbEntitySubEvent.builder().tenantId(tenantId).entityId(entityId).type(type);
        if (!ComponentLifecycleEvent.DELETED.equals(type)) {
            result.info(state.copy());
        }
        return result.build();
    }

    public boolean isNf() {
        return state.notifications;
    }


    public boolean isEmpty() {
        return state.isEmpty();
    }
}
