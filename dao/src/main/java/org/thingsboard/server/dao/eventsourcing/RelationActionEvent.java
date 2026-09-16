// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.eventsourcing;

import lombok.Data;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;

@Data
public class RelationActionEvent {
    private final TenantId tenantId;
    private final EntityRelation relation;
    private final ActionType actionType;
}
