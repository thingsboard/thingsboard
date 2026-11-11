// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.menu;

import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuConfig;
import org.thingsboard.server.common.data.menu.CustomMenuFilter;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CustomMenuService {

    CustomMenu createCustomMenu(CustomMenuInfo customMenuInfo, List<EntityId> assignToList, boolean force) throws ThingsboardException;

    CustomMenu updateCustomMenu(CustomMenu customMenu, boolean force) throws ThingsboardException;

    void updateAssigneeList(CustomMenu oldCustomMenu, CMAssigneeType newAssigneeType, List<EntityId> newAssignToList, String[] newUserGroupNames, boolean force) throws ThingsboardException;

    PageData<CustomMenu> findCustomMenusByTenantId(TenantId tenantId, PageLink pageLink);

    CustomMenuInfo findCustomMenuInfoById(TenantId tenantId, CustomMenuId customMenuId);

    CustomMenu findCustomMenuById(TenantId tenantId, CustomMenuId customMenuId);

    PageData<CustomMenuInfo> findCustomMenuInfos(TenantId tenantId, CustomMenuFilter customMenuFilter, PageLink pageLink);

    CustomMenuConfig findSystemAdminCustomMenuConfig();

    CustomMenuConfig findTenantUserCustomMenuConfig(TenantId tenantId, UserId id);

    CustomMenuConfig findCustomerUserCustomMenuConfig(TenantId tenantId, CustomerId customerId, UserId userId);

    CustomMenu findDefaultCustomMenuByScope(TenantId tenantId, CustomerId customerId, CMScope scope);

    List<EntityInfo> findCustomMenuAssigneeList(CustomMenuInfo customMenuInfo);

    Optional<CustomMenu> findFirstByScopeAndUserGroupNames(TenantId tenantId, CustomerId customerId, CMScope scope, Set<String> userGroupNames);

    CustomMenuDeleteResult deleteCustomMenu(CustomMenu customMenu, boolean force);

    void deleteByTenantId(TenantId tenantId);

}
