/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.audit;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;

import java.util.List;
import java.util.UUID;

public interface AuditLogDao {

    ListenableFuture<Void> saveByTenantId(AuditLog auditLog);

    ListenableFuture<Void> saveByTenantIdAndEntityId(AuditLog auditLog);

    ListenableFuture<Void> saveByTenantIdAndCustomerId(AuditLog auditLog);

    ListenableFuture<Void> saveByTenantIdAndUserId(AuditLog auditLog);

    ListenableFuture<Void> savePartitionsByTenantId(AuditLog auditLog);

    List<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink);

    List<AuditLog> findAuditLogsByTenantIdAndCustomerId(UUID tenantId, CustomerId customerId, List<ActionType> actionTypes, TimePageLink pageLink);

    List<AuditLog> findAuditLogsByTenantIdAndUserId(UUID tenantId, UserId userId, List<ActionType> actionTypes, TimePageLink pageLink);

    List<AuditLog> findAuditLogsByTenantId(UUID tenantId, List<ActionType> actionTypes, TimePageLink pageLink);
}
