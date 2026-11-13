// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RoleControllerTest extends AbstractControllerTest {

    private IdComparator<Role> idComparator;
    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        idComparator = new IdComparator<>();

        savedTenant = saveTenant(getNewTenant("My tenant"));
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    @Test
    public void testFindRoleById() throws Exception {
        Role savedRole = getNewSavedRole("Test role");
        Role foundRole = getRole(savedRole.getId());
        Assert.assertNotNull(foundRole);
        assertEquals(savedRole, foundRole);
    }

    @Test
    public void testSaveRole() throws Exception {
        Role savedRole = getNewSavedRole("Test role");

        Assert.assertNotNull(savedRole);
        Assert.assertNotNull(savedRole.getId());
        Assert.assertTrue(savedRole.getCreatedTime() > 0);
        assertEquals(savedTenant.getId(), savedRole.getTenantId());

        savedRole.setName("New test role");
        doPost("/api/role", savedRole, Role.class);
        Role foundRole = getRole(savedRole.getId());

        assertEquals(foundRole.getName(), savedRole.getName());
    }

    @Test
    public void testChangeRoleType_notAllowed() throws Exception {
        Role role = getNewSavedRole("Test role");
        assertEquals(RoleType.GENERIC, role.getType());

        role.setType(RoleType.GROUP);

        doPost("/api/role", role)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("type cannot be changed")));
        assertEquals(RoleType.GENERIC, getRole(role.getId()).getType());
    }

    @Test
    public void testDeleteRole() throws Exception {
        Role role = getNewSavedRole("Test role");
        Role savedRole = doPost("/api/role", role, Role.class);

        doDelete("/api/role/" + savedRole.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/role/" + savedRole.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveRoleWithEmptyName() throws Exception {
        Role role = new Role();
        role.setType(RoleType.GENERIC);
        doPost("/api/role", role)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Role name should be specified!")));
    }

    @Test
    public void testGetRoles() throws Exception {

        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            roles.add(getNewSavedRole("Test role " + i));
        }
        List<Role> loadedRoles = loadListOf(new PageLink(23), "/api/roles?");

        Collections.sort(roles, idComparator);
        Collections.sort(loadedRoles, idComparator);

        assertEquals(roles, loadedRoles);
    }

    @Test
    public void testGetRolesByName() throws Exception {
        String name1 = "Entity role1";
        List<Role> namesOfRole1 = fillListOf(143, name1);
        List<Role> loadedNamesOfRole1 = loadListOf(new PageLink(15, 0, name1), "/api/roles?");
        Collections.sort(namesOfRole1, idComparator);
        Collections.sort(loadedNamesOfRole1, idComparator);
        assertEquals(namesOfRole1, loadedNamesOfRole1);

        String name2 = "Entity role2";
        List<Role> namesOfRole2 = fillListOf(75, name2);
        List<Role> loadedNamesOfRole2 = loadListOf(new PageLink(4, 0, name2), "/api/roles?");
        Collections.sort(namesOfRole2, idComparator);
        Collections.sort(loadedNamesOfRole2, idComparator);
        assertEquals(namesOfRole2, loadedNamesOfRole2);

        for (Role role : loadedNamesOfRole1) {
            doDelete("/api/role/" + role.getId().getId().toString()).andExpect(status().isOk());
        }
        PageData<Role> pageData = doGetTypedWithPageLink("/api/roles?",
                new TypeReference<PageData<Role>>() {
                }, new PageLink(4, 0, name1));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());

        for (Role role : loadedNamesOfRole2) {
            doDelete("/api/role/" + role.getId().getId().toString()).andExpect(status().isOk());
        }
        pageData = doGetTypedWithPageLink("/api/roles?", new TypeReference<PageData<Role>>() {
        }, new PageLink(4, 0, name2));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());
    }

    private Role getNewSavedRole(String name) throws Exception {
        Role role = createRole(name);
        return doPost("/api/role", role, Role.class);
    }

    private Role createRole(String name) {
        Role role = new Role();
        role.setTenantId(savedTenant.getId());
        role.setName(name);
        role.setType(RoleType.GENERIC);
        return role;
    }

    private Tenant getNewTenant(String title) {
        Tenant tenant = new Tenant();
        tenant.setTitle(title);
        return tenant;
    }

    private Role getRole(RoleId roleId) throws Exception {
        return doGet("/api/role/" + roleId.getId().toString(), Role.class);
    }

    private List<Role> fillListOf(int limit, String partOfName) throws Exception {
        List<Role> roleNames = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String fullName = partOfName + ' ' + StringUtils.randomAlphanumeric(15);
            fullName = i % 2 == 0 ? fullName.toLowerCase() : fullName.toUpperCase();
            Role role = getNewSavedRole(fullName);
            roleNames.add(doPost("/api/role", role, Role.class));
        }
        return roleNames;
    }

    private List<Role> loadListOf(PageLink pageLink, String urlTemplate) throws Exception {
        List<Role> loadedItems = new ArrayList<>();
        PageData<Role> pageData;
        do {
            pageData = doGetTypedWithPageLink(urlTemplate, new TypeReference<PageData<Role>>() {
            }, pageLink);
            loadedItems.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        return loadedItems;
    }
}
