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

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

/**
 * Information about the local websocket subscriptions.
 */
@Builder
@Data
public class TbEntitySubEvent {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final ComponentLifecycleEvent type;
    private final TbSubscriptionsInfo info;
    private final int seqNumber;

    public boolean hasTsOrAttrSub() {
        return info != null && (info.tsAllKeys || info.attrAllKeys || info.tsKeys != null || info.attrKeys != null);
    }
}
