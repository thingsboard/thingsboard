package org.thingsboard.server.service.subscription;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information about the local websocket subscriptions.
 */
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TbSubscriptionsInfo {

    protected boolean notifications;
    protected boolean alarms;
    protected boolean tsAllKeys;
    protected Set<String> tsKeys;
    protected boolean attrAllKeys;
    protected Set<String> attrKeys;

    public boolean isEmpty() {
        return !notifications && !alarms && !tsAllKeys && !attrAllKeys && tsKeys == null && attrKeys == null;
    }

    protected TbSubscriptionsInfo copy() {
        return new TbSubscriptionsInfo(notifications, alarms, tsAllKeys, tsKeys, attrAllKeys, attrKeys);
    }

}
