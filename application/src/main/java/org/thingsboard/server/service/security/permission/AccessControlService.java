// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface AccessControlService {

    void checkPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException;

    boolean hasPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException;

    <I extends EntityId, T extends TenantEntity> void checkPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException;

    <I extends EntityId, T extends TenantEntity> void checkPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity, EntityGroupId entityGroupId) throws ThingsboardException;

    <I extends EntityId, T extends TenantEntity> boolean hasPermission(SecurityUser user, Resource resource, Operation operation, I entityId, T entity) throws ThingsboardException;

    void checkEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) throws ThingsboardException;

    void checkEntityGroupInfoPermission(SecurityUser user, Operation operation, EntityGroupInfo entityGroup) throws ThingsboardException;

    void checkCustomMenuPermission(SecurityUser user, Operation operation, CustomMenuInfo customMenu) throws ThingsboardException;

    boolean hasEntityGroupPermission(SecurityUser user, Operation operation, EntityGroup entityGroup) throws ThingsboardException;

    boolean hasEntityGroupInfoPermission(SecurityUser user, Operation operation, EntityGroupInfo entityGroup) throws ThingsboardException;

}
