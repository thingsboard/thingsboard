// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
