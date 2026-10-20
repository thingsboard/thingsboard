// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.subscription;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Information about the local websocket subscriptions.
 */
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"seqNumber"})
@ToString
@Getter
public class TbSubscriptionsInfo {

    protected boolean notifications;
    protected boolean alarms;
    protected boolean logs;
    protected boolean tsAllKeys;
    protected Set<String> tsKeys;
    protected boolean attrAllKeys;
    protected Set<String> attrKeys;
    protected int seqNumber;

    public boolean isEmpty() {
        return !notifications && !alarms && !logs && !tsAllKeys && !attrAllKeys && tsKeys == null && attrKeys == null;
    }

    protected TbSubscriptionsInfo copy() {
        return copy(0);
    }

    protected TbSubscriptionsInfo copy(int seqNumber) {
        return new TbSubscriptionsInfo(notifications, alarms, logs, tsAllKeys, tsKeys != null ? new HashSet<>(tsKeys) : null, attrAllKeys, attrKeys != null ? new HashSet<>(attrKeys) : null, seqNumber);
    }

}
