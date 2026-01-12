/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.user;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/18/2017.
 */
public class JpaUserDaoTest extends AbstractJpaDaoTest {

    // it comes from the DefaultSystemDataLoaderService super class
    final int COUNT_CREATED_USER = 1;
    final int COUNT_SYSADMIN_USER = 90;
    UUID tenantId;
    UUID customerId;
    @Autowired
    private UserDao userDao;

    @Before
    public void setUp() {
        tenantId = Uuids.timeBased();
        customerId = Uuids.timeBased();
        create30TenantAdminsAnd60CustomerUsers(tenantId, customerId);
    }

    @After
    public void tearDown() {
        delete30TenantAdminsAnd60CustomerUsers(tenantId, customerId);
    }

    @Test
    public void testFindAll() {
        List<User> users = userDao.find(AbstractServiceTest.SYSTEM_TENANT_ID);
        assertEquals(users.size(), COUNT_CREATED_USER + COUNT_SYSADMIN_USER);
    }

    @Test
    public void testFindByEmail() throws JsonProcessingException {
        User user = new User();
        user.setId(new UserId(UUID.randomUUID()));
        user.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        user.setCustomerId(new CustomerId(UUID.randomUUID()));
        user.setEmail("user@thingsboard.org");
        user.setFirstName("Jackson");
        user.setLastName("Roberts");
        String additionalInfo = "{\"key\":\"value-100\"}";
        JsonNode jsonNode = JacksonUtil.toJsonNode(additionalInfo);
        user.setAdditionalInfo(jsonNode);
        userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
        assertEquals(1 + COUNT_SYSADMIN_USER + COUNT_CREATED_USER, userDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());
        User savedUser = userDao.findByEmail(AbstractServiceTest.SYSTEM_TENANT_ID, "user@thingsboard.org");
        assertNotNull(savedUser);
        assertEquals(additionalInfo, savedUser.getAdditionalInfo().toString());
    }

    @Test
    public void testFindTenantAdmins() {
        PageLink pageLink = new PageLink(20);
        PageData<User> tenantAdmins1 = userDao.findTenantAdmins(tenantId, pageLink);
        assertEquals(20, tenantAdmins1.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> tenantAdmins2 = userDao.findTenantAdmins(tenantId,
                pageLink);
        assertEquals(10, tenantAdmins2.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> tenantAdmins3 = userDao.findTenantAdmins(tenantId,
                pageLink);
        assertEquals(0, tenantAdmins3.getData().size());
    }

    @Test
    public void testFindCustomerUsers() {
        PageLink pageLink = new PageLink(40);
        PageData<User> customerUsers1 = userDao.findCustomerUsers(tenantId, customerId, pageLink);
        assertEquals(40, customerUsers1.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> customerUsers2 = userDao.findCustomerUsers(tenantId, customerId,
                pageLink);
        assertEquals(20, customerUsers2.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> customerUsers3 = userDao.findCustomerUsers(tenantId, customerId,
                pageLink);
        assertEquals(0, customerUsers3.getData().size());
    }

    private void create30TenantAdminsAnd60CustomerUsers(UUID tenantId, UUID customerId) {
        // Create 30 tenant admins and 60 customer users
        for (int i = 0; i < 30; i++) {
            saveUser(tenantId, NULL_UUID);
            saveUser(tenantId, customerId);
            saveUser(tenantId, customerId);
        }
    }

    private void saveUser(UUID tenantId, UUID customerId) {
        User user = new User();
        UUID id = Uuids.timeBased();
        user.setId(new UserId(id));
        user.setTenantId(TenantId.fromUUID(tenantId));
        user.setCustomerId(new CustomerId(customerId));
        if (customerId == NULL_UUID) {
            user.setAuthority(Authority.TENANT_ADMIN);
        } else {
            user.setAuthority(Authority.CUSTOMER_USER);
        }
        String idString = id.toString();
        String email = idString.substring(0, idString.indexOf('-')) + "@thingsboard.org";
        user.setEmail(email);
        userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
    }

    private void delete30TenantAdminsAnd60CustomerUsers(UUID tenantId, UUID customerId) {
        List<User> data = userDao.findCustomerUsers(tenantId, customerId, new PageLink(60)).getData();
        data.addAll(userDao.findTenantAdmins(tenantId, new PageLink(30)).getData());
        for (User user : data) {
            userDao.removeById(user.getTenantId(), user.getUuidId());
        }
    }
}
