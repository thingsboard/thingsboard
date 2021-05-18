/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.common.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HashMap that removed eldest (by put order) entries
 * It guaranteed that size is not greater then maxEntries parameter. And remove time is constant O(1).
 * Use withMaxEntries to setup maxEntries.
 * Because overloaded constructor will look similar to LinkedHashMap(initCapacity)
 * Example:
 *   new LinkedHashMapRemoveEldest<Long, String>().withMaxEntries(100L)
 * */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LinkedHashMapRemoveEldest<K,V> extends LinkedHashMap<K,V> {
    long maxEntries = Long.MAX_VALUE;

    public LinkedHashMapRemoveEldest() {
        super();
    }

    public LinkedHashMapRemoveEldest(int initialCapacity) {
        super(initialCapacity);
    }

    public LinkedHashMapRemoveEldest(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public LinkedHashMapRemoveEldest(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public LinkedHashMapRemoveEldest(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor, accessOrder);
    }

    public LinkedHashMapRemoveEldest<K,V> withMaxEntries(long maxEntries) {
        this.maxEntries = maxEntries;
        return this;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxEntries;
    }
}
