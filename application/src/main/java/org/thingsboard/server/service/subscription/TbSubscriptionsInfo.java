package org.thingsboard.server.service.subscription;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information about the local websocket subscriptions.
 */
@RequiredArgsConstructor
public class TbSubscriptionsInfo {

    protected boolean notifications;
    protected boolean alarms;
    protected boolean tsAllKeys;
    protected Set<String> tsKeys;
    protected boolean attrAllKeys;
    protected Set<String> attrKeys;

    public void update(TbEntitySubEvent event) {
        if (event.getNotifications() != null) {
            this.notifications = event.getNotifications();
        }
        if (event.getAlarms() != null) {
            this.alarms = event.getAlarms();
        }
        if (event.getTsAllKeys() != null) {
            this.tsAllKeys = event.getTsAllKeys();
            if (this.tsAllKeys) {
                this.tsKeys = null;
            }
        }
        if (!this.tsAllKeys && event.getTsKeys() != null) {
            if (event.getTsKeys().isEmpty()) {
                tsKeys = null;
            } else {
                if (tsKeys == null) {
                    tsKeys = ConcurrentHashMap.newKeySet();
                }
                tsKeys.addAll(event.getTsKeys());
            }
        }
        if (event.getAttrAllKeys() != null) {
            this.attrAllKeys = event.getAttrAllKeys();
            if (this.attrAllKeys) {
                this.attrKeys = null;
            }
        }
        if (!this.attrAllKeys && event.getAttrKeys() != null) {
            if (event.getAttrKeys().isEmpty()) {
                attrKeys = null;
            } else {
                if (attrKeys == null) {
                    attrKeys = ConcurrentHashMap.newKeySet();
                }
                attrKeys.addAll(event.getAttrKeys());
            }
        }
    }

    public boolean isEmpty() {
        return !notifications && !alarms && !tsAllKeys && !attrAllKeys && tsKeys == null && attrKeys == null;
    }
}
