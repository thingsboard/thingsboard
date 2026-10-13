// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.entitiy.user;

import jakarta.servlet.http.HttpServletRequest;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserActivationLink;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;

import java.util.List;

public interface TbUserService {

    User save(TenantId tenantId, CustomerId customerId, Authority authority, User tbUser,
              boolean sendActivationMail, HttpServletRequest request,
              EntityGroup entityGroup, User user) throws ThingsboardException;

    User save(TenantId tenantId, CustomerId customerId, Authority authority, User tbUser,
              boolean sendActivationMail, HttpServletRequest request,
              List<EntityGroup> entityGroups, User user) throws ThingsboardException;

    void delete(TenantId tenantId, CustomerId customerId, User user, User responsibleUser) throws ThingsboardException;

    UserActivationLink getActivationLink(TenantId tenantId, CustomerId customerId, Authority authority, UserId userId, HttpServletRequest request) throws ThingsboardException;

}
