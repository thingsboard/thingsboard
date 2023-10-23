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
                subs.put(serviceId, event.getInfo());
                break;
            case UPDATED:
                var newSubInfo = event.getInfo();
                if (newSubInfo.isEmpty()) {
                    subs.remove(serviceId);
                    return isEmpty();
                } else {
                    subs.put(serviceId, newSubInfo);
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
