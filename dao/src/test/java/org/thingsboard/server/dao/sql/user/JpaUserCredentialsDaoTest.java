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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.user.UserCredentialsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
public class JpaUserCredentialsDaoTest extends AbstractJpaDaoTest {

    public static final String ACTIVATE_TOKEN = "ACTIVATE_TOKEN_0";
    public static final String RESET_TOKEN = "RESET_TOKEN_0";
    public static final int COUNT_USER_CREDENTIALS = 2;
    List<UserCredentials> userCredentialsList;
    UserCredentials neededUserCredentials;

    @Autowired
    private UserCredentialsDao userCredentialsDao;

    @Before
    public void setUp() {
        userCredentialsList = new ArrayList<>();
        for (int i=0; i<COUNT_USER_CREDENTIALS; i++) {
            userCredentialsList.add(createUserCredentials(i));
        }
        neededUserCredentials = userCredentialsList.get(0);
        assertNotNull(neededUserCredentials);
    }

    UserCredentials createUserCredentials(int number) {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        userCredentials.setUserId(new UserId(UUID.randomUUID()));
        userCredentials.setPassword("password");
        userCredentials.setActivateToken("ACTIVATE_TOKEN_" + number);
        userCredentials.setActivateTokenExpTime(123L);
        userCredentials.setResetToken("RESET_TOKEN_" + number);
        userCredentials.setResetTokenExpTime(321L);
        return userCredentialsDao.save(SYSTEM_TENANT_ID, userCredentials);
    }

    @After
    public void after() {
        for (UserCredentials userCredentials : userCredentialsList) {
            userCredentialsDao.removeById(TenantId.SYS_TENANT_ID, userCredentials.getUuidId());
        }
    }

    @Test
    public void testFindAll() {
        List<UserCredentials> userCredentials = userCredentialsDao.find(SYSTEM_TENANT_ID);
        assertEquals(COUNT_USER_CREDENTIALS + 1, userCredentials.size());
    }

    @Test
    public void testFindByUserId() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByUserId(SYSTEM_TENANT_ID, neededUserCredentials.getUserId().getId());
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials, foundedUserCredentials);
    }

    @Test
    public void testFindByActivateToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByActivateToken(SYSTEM_TENANT_ID, ACTIVATE_TOKEN);
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }

    @Test
    public void testFindByResetToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByResetToken(SYSTEM_TENANT_ID, RESET_TOKEN);
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }
}
