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
package org.thingsboard.server.dao.sql.device;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.device.DeviceCredentialsDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaDeviceCredentialsDaoTest extends AbstractJpaDaoTest {

    @Autowired
    DeviceCredentialsDao deviceCredentialsDao;

    List<DeviceCredentials> deviceCredentialsList;
    DeviceCredentials neededDeviceCredentials;

    @Before
    public void setUp() {
        deviceCredentialsList = List.of(createAndSaveDeviceCredentials(), createAndSaveDeviceCredentials());
        neededDeviceCredentials = deviceCredentialsList.get(0);
        assertNotNull(neededDeviceCredentials);
    }

    DeviceCredentials createAndSaveDeviceCredentials() {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(UUID.randomUUID().toString());
        deviceCredentials.setCredentialsValue("CHECK123");
        deviceCredentials.setDeviceId(new DeviceId(UUID.randomUUID()));
        return deviceCredentialsDao.save(TenantId.SYS_TENANT_ID, deviceCredentials);
    }

    @After
    public void deleteDeviceCredentials() {
        for (DeviceCredentials credentials : deviceCredentialsList) {
            deviceCredentialsDao.removeById(TenantId.SYS_TENANT_ID, credentials.getUuidId());
        }
    }

    @Test
    public void testFindByDeviceId() {
        DeviceCredentials foundedDeviceCredentials = deviceCredentialsDao.findByDeviceId(SYSTEM_TENANT_ID, neededDeviceCredentials.getDeviceId().getId());
        assertNotNull(foundedDeviceCredentials);
        assertEquals(neededDeviceCredentials.getId(), foundedDeviceCredentials.getId());
        assertEquals(neededDeviceCredentials.getCredentialsId(), foundedDeviceCredentials.getCredentialsId());
    }

    @Test
    public void findByCredentialsId() {
        DeviceCredentials foundedDeviceCredentials = deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, neededDeviceCredentials.getCredentialsId());
        assertNotNull(foundedDeviceCredentials);
        assertEquals(neededDeviceCredentials.getId(), foundedDeviceCredentials.getId());
    }
}
