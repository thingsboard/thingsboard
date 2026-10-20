// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.custommenu;

import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.dao.menu.CustomMenuCacheKey;

import java.util.List;

public interface TbCustomMenuService extends EtagCacheService<CustomMenuCacheKey> {

    CustomMenu createCustomMenu(CustomMenuInfo customMenuInfo, List<EntityId> assignToList, boolean force) throws ThingsboardException;

    CustomMenu updateCustomMenu(CustomMenu customMenu, boolean force) throws ThingsboardException;

    void updateAssigneeList(CustomMenu oldCustomMenu, CMAssigneeType newAssigneeType, List<EntityId> newAssignToList, String[] userGroupNames, boolean force) throws ThingsboardException;

    CustomMenuDeleteResult deleteCustomMenu(CustomMenu customMenu, boolean force);
}
