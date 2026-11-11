// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.CreateCustomMenuArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteCustomMenuArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomMenuArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomMenuAssigneeListArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomMenuConfigArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomMenuInfoByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomMenuInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateCustomMenuAssigneeListArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateCustomMenuConfigArgs;
import org.thingsboard.client.api.ThingsboardApi.UpdateCustomMenuNameArgs;
import org.thingsboard.client.model.CMAssigneeType;
import org.thingsboard.client.model.CMScope;
import org.thingsboard.client.model.CustomMenu;
import org.thingsboard.client.model.CustomMenuConfig;
import org.thingsboard.client.model.CustomMenuDeleteResult;
import org.thingsboard.client.model.CustomMenuInfo;
import org.thingsboard.client.model.EntityInfo;
import org.thingsboard.client.model.PageDataCustomMenuInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class CustomMenuApiClientTest extends AbstractApiClientTest {

    @Test
    public void testCustomMenuLifecycle() throws Exception {
        long ts = System.currentTimeMillis();
        String name = TEST_PREFIX + ts;

        CustomMenu created = client.createCustomMenu(CreateCustomMenuArgs.builder()
                .customMenuInfo(buildCustomMenuInfo(name))
                .build());
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(name, created.getName());
        assertEquals(CMScope.TENANT, created.getScope());

        UUID menuId = created.getId().getId();

        CustomMenuInfo fetched = client.getCustomMenuInfoById(GetCustomMenuInfoByIdArgs.builder()
                .customMenuId(menuId)
                .build());
        assertNotNull(fetched);
        assertEquals(menuId, fetched.getId().getId());
        assertEquals(name, fetched.getName());

        String updatedName = name + "_updated";
        client.updateCustomMenuName(UpdateCustomMenuNameArgs.builder()
                .customMenuId(menuId)
                .body(updatedName)
                .build());
        CustomMenuInfo afterRename = client.getCustomMenuInfoById(GetCustomMenuInfoByIdArgs.builder()
                .customMenuId(menuId)
                .build());
        assertEquals(updatedName, afterRename.getName());

        CustomMenu afterConfigUpdate = client.updateCustomMenuConfig(UpdateCustomMenuConfigArgs.builder()
                .customMenuId(menuId)
                .customMenuConfig(new CustomMenuConfig())
                .build());
        assertNotNull(afterConfigUpdate);
        assertNotNull(afterConfigUpdate.getConfig());

        CustomMenuDeleteResult deleteResult = client.deleteCustomMenu(DeleteCustomMenuArgs.builder()
                .customMenuId(menuId)
                .force(true)
                .build());
        assertNotNull(deleteResult);
        assertEquals(Boolean.TRUE, deleteResult.getSuccess());

        assertReturns404(() -> client.getCustomMenuInfoById(GetCustomMenuInfoByIdArgs.builder()
                .customMenuId(menuId)
                .build()));
    }

    @Test
    public void testGetCustomMenuInfos() throws Exception {
        long ts = System.currentTimeMillis();
        List<UUID> createdIds = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            CustomMenu created = client.createCustomMenu(CreateCustomMenuArgs.builder()
                    .customMenuInfo(buildCustomMenuInfo(TEST_PREFIX + ts + "_" + i))
                    .build());
            createdIds.add(created.getId().getId());
        }

        PageDataCustomMenuInfo allPage = client.getCustomMenuInfos(GetCustomMenuInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allPage);
        assertNotNull(allPage.getData());
        assertTrue(allPage.getTotalElements() >= 5);

        PageDataCustomMenuInfo byScope = client.getCustomMenuInfos(GetCustomMenuInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .scope(CMScope.TENANT)
                .build());
        assertTrue(byScope.getTotalElements() >= 5);

        PageDataCustomMenuInfo byText = client.getCustomMenuInfos(GetCustomMenuInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .textSearch(TEST_PREFIX + ts)
                .build());
        assertEquals(5, byText.getData().size());

        PageDataCustomMenuInfo page1 = client.getCustomMenuInfos(GetCustomMenuInfosArgs.builder()
                .pageSize(2)
                .page(0)
                .textSearch(TEST_PREFIX + ts)
                .build());
        assertEquals(2, page1.getData().size());
        assertTrue(page1.getHasNext());

        for (UUID id : createdIds) {
            client.deleteCustomMenu(DeleteCustomMenuArgs.builder()
                    .customMenuId(id)
                    .force(true)
                    .build());
        }
    }

    @Test
    public void testGetAndUpdateCustomMenuConfig() throws Exception {
        long ts = System.currentTimeMillis();
        CustomMenu created = client.createCustomMenu(CreateCustomMenuArgs.builder()
                .customMenuInfo(buildCustomMenuInfo(TEST_PREFIX + ts))
                .build());
        UUID menuId = created.getId().getId();

        CustomMenuConfig initial = client.getCustomMenuConfig(GetCustomMenuConfigArgs.builder()
                .customMenuId(menuId)
                .build());
        assertNotNull(initial);

        client.updateCustomMenuConfig(UpdateCustomMenuConfigArgs.builder()
                .customMenuId(menuId)
                .customMenuConfig(new CustomMenuConfig())
                .build());
        CustomMenuConfig updated = client.getCustomMenuConfig(GetCustomMenuConfigArgs.builder()
                .customMenuId(menuId)
                .build());
        assertNotNull(updated);

        client.deleteCustomMenu(DeleteCustomMenuArgs.builder()
                .customMenuId(menuId)
                .force(true)
                .build());
    }

    @Test
    public void testGetCustomMenuAssigneeList() throws Exception {
        long ts = System.currentTimeMillis();
        CustomMenu created = client.createCustomMenu(CreateCustomMenuArgs.builder()
                .customMenuInfo(buildCustomMenuInfo(TEST_PREFIX + ts))
                .build());
        UUID menuId = created.getId().getId();

        List<EntityInfo> initial = client.getCustomMenuAssigneeList(GetCustomMenuAssigneeListArgs.builder()
                .customMenuId(menuId)
                .build());
        assertNotNull(initial);
        assertTrue(initial.isEmpty());

        client.updateCustomMenuAssigneeList(UpdateCustomMenuAssigneeListArgs.builder()
                .id(menuId)
                .assigneeType(CMAssigneeType.ALL)
                .force(false)
                .build());
        List<EntityInfo> afterAll = client.getCustomMenuAssigneeList(GetCustomMenuAssigneeListArgs.builder()
                .customMenuId(menuId)
                .build());
        assertNotNull(afterAll);

        client.deleteCustomMenu(DeleteCustomMenuArgs.builder()
                .customMenuId(menuId)
                .force(true)
                .build());
    }

    @Test
    public void testGetCustomMenuDoesNotThrow() throws Exception {
        client.getCustomMenu(GetCustomMenuArgs.builder()

                .build());
    }

    private CustomMenuInfo buildCustomMenuInfo(String name) {
        CustomMenuInfo info = new CustomMenuInfo();
        info.setName(name);
        info.setScope(CMScope.TENANT);
        info.setAssigneeType(CMAssigneeType.NO_ASSIGN);
        return info;
    }

}
