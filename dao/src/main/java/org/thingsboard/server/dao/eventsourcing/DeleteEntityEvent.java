// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.eventsourcing;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

@Builder
@Data
public class DeleteEntityEvent<T> {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final T entity;
    private final String body;
    private final ActionCause cause;

    @Builder.Default
    private final long ts = System.currentTimeMillis();

}
