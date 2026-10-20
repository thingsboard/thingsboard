// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.entity.group;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbEntityGroupService {

    EntityGroupInfo save(TenantId tenantId, EntityId parentEntityId, EntityGroup entity, User currentUser) throws Exception;

    void delete(TenantId tenantId, EntityGroup entityGroup, User user) throws ThingsboardException;

    EntityId makePublic(TenantId tenantId, EntityGroup entityGroup, User user) throws ThingsboardException;

    void makePrivate(TenantId tenantId, EntityGroup entityGroup, User user) throws ThingsboardException;
}
