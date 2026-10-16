// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.entity.relation;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;

public interface TbEntityRelationService {

    EntityRelation save(TenantId tenantId, CustomerId customerId, EntityRelation entity, User user) throws ThingsboardException;

    EntityRelation delete(TenantId tenantId, CustomerId customerId, EntityRelation entity, User user) throws ThingsboardException;

    void deleteCommonRelations(TenantId tenantId, CustomerId customerId, EntityId entityId, User user) throws ThingsboardException;

}
