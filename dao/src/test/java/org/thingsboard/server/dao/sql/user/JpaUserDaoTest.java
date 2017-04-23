/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.user.UserDao;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by Valerii Sosliuk on 4/18/2017.
 */
public class JpaUserDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private UserDao userDao;

    @Test
    @DatabaseSetup("classpath:dbunit/users.xml")
    public void testFindAll() {
        List<User> users = userDao.find();
        assertEquals(users.size(), 5);
    }

    @Test
    @DatabaseSetup("classpath:dbunit/users.xml")
    public void findByEmail() {
        User user = userDao.findByEmail("sysadm@thingsboard.org");
        assertNotNull("User is expected to be not null", user);
        assertEquals("9cb58ba0-27c1-11e7-93ae-92361f002671", user.getId().toString());
        assertEquals("c97ea14e-27c1-11e7-93ae-92361f002671", user.getTenantId().toString());
        assertEquals("cdf9c79e-27c1-11e7-93ae-92361f002671", user.getCustomerId().toString());
        assertEquals(Authority.SYS_ADMIN, user.getAuthority());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("{\"key\":\"value-0\"}", user.getAdditionalInfo().toString());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/users.xml")
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
        userDao.save(user);
        assertEquals(6, userDao.find().size());
        User savedUser = userDao.findByEmail("user@thingsboard.org");
        assertNotNull(savedUser);
    }
}
