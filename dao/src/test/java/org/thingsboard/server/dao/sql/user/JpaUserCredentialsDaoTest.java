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

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.user.UserCredentialsDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
public class JpaUserCredentialsDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private UserCredentialsDao userCredentialsDao;

    @Test
    @DatabaseSetup("classpath:dbunit/user_credentials.xml")
    public void testFindAll() {
        List<UserCredentials> userCredentials = userCredentialsDao.find();
        assertEquals(2, userCredentials.size());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/user_credentials.xml")
    public void testFindByUserId() {
        UserCredentials userCredentials = userCredentialsDao.findByUserId(UUID.fromString("787827e6-27d7-11e7-93ae-92361f002671"));
        assertNotNull(userCredentials);
        assertEquals("4b9e010c-27d5-11e7-93ae-92361f002671", userCredentials.getId().toString());
        assertEquals(true, userCredentials.isEnabled());
        assertEquals("password", userCredentials.getPassword());
        assertEquals("ACTIVATE_TOKEN_2", userCredentials.getActivateToken());
        assertEquals("RESET_TOKEN_2", userCredentials.getResetToken());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/user_credentials.xml")
    public void testFindByActivateToken() {
        UserCredentials userCredentials = userCredentialsDao.findByActivateToken("ACTIVATE_TOKEN_1");
        assertNotNull(userCredentials);
        assertEquals("3ed10af0-27d5-11e7-93ae-92361f002671", userCredentials.getId().toString());
    }

    @Test
    @DatabaseSetup("classpath:dbunit/user_credentials.xml")
    public void testFindByResetToken() {
        UserCredentials userCredentials = userCredentialsDao.findByResetToken("RESET_TOKEN_2");
        assertNotNull(userCredentials);
        assertEquals("4b9e010c-27d5-11e7-93ae-92361f002671", userCredentials.getId().toString());
    }
}
