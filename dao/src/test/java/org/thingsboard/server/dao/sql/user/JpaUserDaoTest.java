/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/18/2017.
 */
public class JpaUserDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    @DatabaseSetup("classpath:dbunit/user.xml")
    public void testFindAll() {
        List<User> users = userDao.find(AbstractServiceTest.SYSTEM_TENANT_ID);
        assertEquals(users.size(), 5);
    }

    @Test
    @DatabaseSetup("classpath:dbunit/user.xml")
    public void testFindByEmail() {
        User user = userDao.findByEmail(AbstractServiceTest.SYSTEM_TENANT_ID,"sysadm@thingsboard.org");
        assertNotNull("User is expected to be not null", user);
        assertEquals("9cb58ba0-27c1-11e7-93ae-92361f002671", user.getId().toString());
        assertEquals("c97ea14e-27c1-11e7-93ae-92361f002671", user.getTenantId().toString());
        assertEquals("cdf9c79e-27c1-11e7-93ae-92361f002671", user.getCustomerId().toString());
        assertEquals(Authority.SYS_ADMIN, user.getAuthority());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindTenantAdmins() {
        UUID tenantId = UUIDs.timeBased();
        UUID customerId = UUIDs.timeBased();
        create30Adminsand60Users(tenantId, customerId);
        List<User> tenantAdmins1 = userDao.findTenantAdmins(tenantId, new TextPageLink(20));
        assertEquals(20, tenantAdmins1.size());
        List<User> tenantAdmins2 = userDao.findTenantAdmins(tenantId,
                new TextPageLink(20, null, tenantAdmins1.get(19).getId().getId(), null));
        assertEquals(10, tenantAdmins2.size());
        List<User> tenantAdmins3 = userDao.findTenantAdmins(tenantId,
                new TextPageLink(20, null, tenantAdmins2.get(9).getId().getId(), null));
        assertEquals(0, tenantAdmins3.size());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/empty_dataset.xml")
    public void testFindCustomerUsers() {
        UUID tenantId = UUIDs.timeBased();
        UUID customerId = UUIDs.timeBased();
        create30Adminsand60Users(tenantId, customerId);
        List<User> customerUsers1 = userDao.findCustomerUsers(tenantId, customerId, new TextPageLink(40));
        assertEquals(40, customerUsers1.size());
        List<User> customerUsers2 = userDao.findCustomerUsers(tenantId, customerId,
                new TextPageLink(20, null, customerUsers1.get(39).getId().getId(), null));
        assertEquals(20, customerUsers2.size());
        List<User> customerUsers3 = userDao.findCustomerUsers(tenantId, customerId,
                new TextPageLink(20, null, customerUsers2.get(19).getId().getId(), null));
        assertEquals(0, customerUsers3.size());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/user.xml")
    public void testSave() throws IOException {
        User user = new User();
        user.setId(new UserId(UUID.fromString("cd481534-27cc-11e7-93ae-92361f002671")));
        user.setTenantId(new TenantId(UUID.fromString("1edcb2c6-27cb-11e7-93ae-92361f002671")));
        user.setCustomerId(new CustomerId(UUID.fromString("51477cb4-27cb-11e7-93ae-92361f002671")));
        user.setEmail("user@thingsboard.org");
        user.setFirstName("Jackson");
        user.setLastName("Roberts");
        ObjectMapper mapper = new ObjectMapper();
        String additionalInfo = "{\"key\":\"value-100\"}";
        JsonNode jsonNode = mapper.readTree(additionalInfo);
        user.setAdditionalInfo(jsonNode);
        userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID,user);
        assertEquals(6, userDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());
        User savedUser = userDao.findByEmail(AbstractServiceTest.SYSTEM_TENANT_ID,"user@thingsboard.org");
        assertNotNull(savedUser);
        assertEquals(additionalInfo, savedUser.getAdditionalInfo().toString());
    }

    private void create30Adminsand60Users(UUID tenantId, UUID customerId) {
        // Create 30 tenant admins and 60 customer users
        for (int i = 0; i < 30; i++) {
            saveUser(tenantId, NULL_UUID);
            saveUser(tenantId, customerId);
            saveUser(tenantId, customerId);
        }
    }

    private void saveUser(UUID tenantId, UUID customerId) {
        User user = new User();
        UUID id = UUIDs.timeBased();
        user.setId(new UserId(id));
        user.setTenantId(new TenantId(tenantId));
        user.setCustomerId(new CustomerId(customerId));
        if (customerId == NULL_UUID) {
            user.setAuthority(Authority.TENANT_ADMIN);
        } else {
            user.setAuthority(Authority.CUSTOMER_USER);
        }
        String idString = id.toString();
        String email = idString.substring(0, idString.indexOf('-')) + "@thingsboard.org";
        user.setEmail(email);
        userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID,user);
    }
}
