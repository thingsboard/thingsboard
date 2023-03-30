/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserSettingsDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

public class JpaUserSettingsDaoTest extends AbstractJpaDaoTest {

    private UUID tenantId;
    private User user;

    @Autowired
    private UserSettingsDao userSettingsDao;

    @Autowired
    private UserDao userDao;

    @Before
    public void setUp() {
        tenantId = Uuids.timeBased();
        user = saveUser(tenantId, Uuids.timeBased());
    }

    @After
    public void tearDown() {
        userDao.removeById(user.getTenantId(), user.getUuidId());
    }

    @Test
    public void testFindSettingsByUserId() {
        UserSettings userSettings = createUserSettings(user.getId());

        UserSettings retrievedUserSettings = userSettingsDao.findById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettings.GENERAL));
        assertEquals(retrievedUserSettings.getSettings(), userSettings.getSettings());

        userSettingsDao.removeById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettings.GENERAL));

        UserSettings retrievedUserSettings2 = userSettingsDao.findById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettings.GENERAL));
        assertNull(retrievedUserSettings2);
    }

    private UserSettings createUserSettings(UserId userId) {
        UserSettings userSettings = new UserSettings();
        userSettings.setType(UserSettings.GENERAL);
        userSettings.setSettings(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)));
        userSettings.setUserId(userId);
        return userSettingsDao.save(SYSTEM_TENANT_ID, userSettings);
    }

    private User saveUser(UUID tenantId, UUID customerId) {
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
        return  userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
    }
}
