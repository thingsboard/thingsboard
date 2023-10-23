/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
