/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Set<TbSubscription<?>> subs = ConcurrentHashMap.newKeySet();
    private volatile TbSubscriptionsInfo state = new TbSubscriptionsInfo();

    private final Map<Integer, Set<TbSubscription<?>>> pendingSubs = new ConcurrentHashMap<>();
    @Getter
    @Setter
    private int pendingTimeSeriesEvent;
    @Getter
    @Setter
    private long pendingTimeSeriesEventTs;
    @Getter
    @Setter
    private int pendingAttributesEvent;
    @Getter
    @Setter
    private long pendingAttributesEventTs;

    private int seqNumber = 0;

    public TbEntitySubEvent add(TbSubscription<?> subscription) {
        log.trace("[{}][{}][{}] Adding: {}", tenantId, entityId, subscription.getSubscriptionId(), subscription);
        boolean created = subs.isEmpty();
        subs.add(subscription);
        TbSubscriptionsInfo newState = created ? state : state.copy();
        boolean stateChanged = false;
        switch (subscription.getType()) {
            case NOTIFICATIONS:
            case NOTIFICATIONS_COUNT:
                if (!newState.notifications) {
                    newState.notifications = true;
                    stateChanged = true;
                }
                break;
            case ALARMS:
                if (!newState.alarms) {
                    newState.alarms = true;
                    stateChanged = true;
                }
                break;
            case ATTRIBUTES:
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
                break;
            case TIMESERIES:
                var tsSub = (TbTimeSeriesSubscription) subscription;
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
                break;
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
        if (isEmpty()) {
            return toEvent(ComponentLifecycleEvent.DELETED);
        }
        TbSubscriptionType type = sub.getType();
        TbSubscriptionsInfo newState = state.copy();
        clearState(newState, type);
        return updateState(Set.of(type), newState);
    }

    public TbEntitySubEvent removeAll(List<? extends TbSubscription<?>> subsToRemove) {
        Set<TbSubscriptionType> changedTypes = new HashSet<>();
        TbSubscriptionsInfo newState = state.copy();
        for (TbSubscription<?> sub : subsToRemove) {
            log.trace("[{}][{}][{}] Removing: {}", tenantId, entityId, sub.getSubscriptionId(), sub);
            if (!subs.remove(sub)) {
                continue;
            }
            if (isEmpty()) {
                return toEvent(ComponentLifecycleEvent.DELETED);
            }
            TbSubscriptionType type = sub.getType();
            if (changedTypes.contains(type)) {
                continue;
            }

            clearState(newState, type);
            changedTypes.add(type);
        }

        return updateState(changedTypes, newState);
    }

    private void clearState(TbSubscriptionsInfo state, TbSubscriptionType type) {
        switch (type) {
            case NOTIFICATIONS:
            case NOTIFICATIONS_COUNT:
                state.notifications = false;
                break;
            case ALARMS:
                state.alarms = false;
                break;
            case ATTRIBUTES:
                state.attrAllKeys = false;
                state.attrKeys = null;
                break;
            case TIMESERIES:
                state.tsAllKeys = false;
                state.tsKeys = null;
        }
    }

    private TbEntitySubEvent updateState(Set<TbSubscriptionType> updatedTypes, TbSubscriptionsInfo newState) {
        for (TbSubscription<?> subscription : subs) {
            TbSubscriptionType type = subscription.getType();
            if (!updatedTypes.contains(type)) {
                continue;
            }
            switch (type) {
                case NOTIFICATIONS:
                case NOTIFICATIONS_COUNT:
                    if (!newState.notifications) {
                        newState.notifications = true;
                    }
                    break;
                case ALARMS:
                    if (!newState.alarms) {
                        newState.alarms = true;
                    }
                    break;
                case ATTRIBUTES:
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
                    break;
                case TIMESERIES:
                    var tsSub = (TbTimeSeriesSubscription) subscription;
                    if (!newState.tsAllKeys && tsSub.isAllKeys()) {
                        newState.tsAllKeys = true;
                        continue;
                    }
                    if (newState.tsKeys == null) {
                        newState.tsKeys = new HashSet<>(tsSub.getKeyStates().keySet());
                    } else {
                        newState.tsKeys.addAll(tsSub.getKeyStates().keySet());
                    }
                    break;
            }
        }
        if (newState.equals(state)) {
            return null;
        } else {
            this.state = newState;
            return toEvent(ComponentLifecycleEvent.UPDATED);
        }
    }

    public TbEntitySubEvent toEvent(ComponentLifecycleEvent type) {
        seqNumber++;
        var result = TbEntitySubEvent.builder().tenantId(tenantId).entityId(entityId).type(type).seqNumber(seqNumber);
        if (!ComponentLifecycleEvent.DELETED.equals(type)) {
            result.info(state.copy(seqNumber));
        }
        return result.build();
    }

    public boolean isNf() {
        return state.notifications;
    }


    public boolean isEmpty() {
        return subs.isEmpty();
    }

    public TbSubscription<?> registerPendingSubscription(TbSubscription<?> subscription, TbEntitySubEvent event) {
        if (TbSubscriptionType.ATTRIBUTES.equals(subscription.getType())) {
            if (event != null) {
                log.trace("[{}][{}] Registering new pending attributes subscription event: {} for subscription: {}", tenantId, entityId, event.getSeqNumber(), subscription.getSubscriptionId());
                pendingAttributesEvent = event.getSeqNumber();
                pendingAttributesEventTs = System.currentTimeMillis();
                pendingSubs.computeIfAbsent(pendingAttributesEvent, e -> new HashSet<>()).add(subscription);
            } else if (pendingAttributesEvent > 0) {
                log.trace("[{}][{}] Registering pending attributes subscription {} for event: {} ", tenantId, entityId, subscription.getSubscriptionId(), pendingAttributesEvent);
                pendingSubs.computeIfAbsent(pendingAttributesEvent, e -> new HashSet<>()).add(subscription);
            } else {
                return subscription;
            }
        } else if (subscription instanceof TbTimeSeriesSubscription) {
            if (event != null) {
                log.trace("[{}][{}] Registering new pending time-series subscription event: {} for subscription: {}", tenantId, entityId, event.getSeqNumber(), subscription.getSubscriptionId());
                pendingTimeSeriesEvent = event.getSeqNumber();
                pendingTimeSeriesEventTs = System.currentTimeMillis();
                pendingSubs.computeIfAbsent(pendingTimeSeriesEvent, e -> new HashSet<>()).add(subscription);
            } else if (pendingTimeSeriesEvent > 0) {
                log.trace("[{}][{}] Registering pending time-series subscription {} for event: {} ", tenantId, entityId, subscription.getSubscriptionId(), pendingTimeSeriesEvent);
                pendingSubs.computeIfAbsent(pendingTimeSeriesEvent, e -> new HashSet<>()).add(subscription);
            } else {
                return subscription;
            }
        }
        return null;
    }

    public Set<TbSubscription<?>> clearPendingSubscriptions(int seqNumber) {
        if (pendingTimeSeriesEvent == seqNumber) {
            pendingTimeSeriesEvent = 0;
            pendingTimeSeriesEventTs = 0L;
        } else if (pendingAttributesEvent == seqNumber) {
            pendingAttributesEvent = 0;
            pendingAttributesEventTs = 0L;
        }
        return pendingSubs.remove(seqNumber);
    }
}
