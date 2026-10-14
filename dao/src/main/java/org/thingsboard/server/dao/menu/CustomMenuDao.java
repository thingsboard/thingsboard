// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.menu;

import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuFilter;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.Optional;
import java.util.Set;


public interface CustomMenuDao extends Dao<CustomMenu> {

    CustomMenuInfo findInfoById(CustomMenuId customMenuId);

    PageData<CustomMenuInfo> findInfosByFilter(TenantId tenantId, CustomMenuFilter customMenuFilter, PageLink pageLink);

    CustomMenu findDefaultMenuByScope(TenantId tenantId, CustomerId customerId, CMScope scope);

    void removeByTenantId(TenantId tenantId);

    PageData<CustomMenu> findByTenantId(TenantId tenantId, PageLink pageLink);

    Optional<CustomMenu> findFirstByScopeAndUserGroupNames(TenantId tenantId, CustomerId customerId, CMScope scope, Set<String> userGroupNames);
}
