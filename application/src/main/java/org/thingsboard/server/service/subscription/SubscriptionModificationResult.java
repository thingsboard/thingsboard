// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * The modification result of entity subscription
 */
@Builder
@Data
public class SubscriptionModificationResult {

    private TenantId tenantId;
    private EntityId entityId;
    private TbSubscription<?> subscription;
    private TbSubscription<?> missedUpdatesCandidate;
    private TbEntitySubEvent event;

    public boolean hasEvent() {
        return event != null;
    }
}
