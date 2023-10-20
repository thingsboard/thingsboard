package org.thingsboard.server.service.subscription;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Set;

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

}
