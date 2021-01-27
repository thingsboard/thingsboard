/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsDao;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class BaseDeviceCredentialsCacheTest extends AbstractServiceTest {

    private static final String CREDENTIALS_ID_1 = RandomStringUtils.randomAlphanumeric(20);
    private static final String CREDENTIALS_ID_2 = RandomStringUtils.randomAlphanumeric(20);

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    private DeviceCredentialsDao deviceCredentialsDao;
    private DeviceService deviceService;

    @Autowired
    private CacheManager cacheManager;

    private UUID deviceId = UUID.randomUUID();

    @Before
    public void setup() throws Exception {
        deviceCredentialsDao = mock(DeviceCredentialsDao.class);
        deviceService = mock(DeviceService.class);
        ReflectionTestUtils.setField(unwrapDeviceCredentialsService(), "deviceCredentialsDao", deviceCredentialsDao);
        ReflectionTestUtils.setField(unwrapDeviceCredentialsService(), "deviceService", deviceService);
    }

    @After
    public void cleanup() {
        cacheManager.getCache(CacheConstants.DEVICE_CREDENTIALS_CACHE).clear();
    }

    @Test
    public void testFindDeviceCredentialsByCredentialsId_Cached() {
        when(deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);
    }

    @Test
    public void testDeleteDeviceCredentials_EvictsCache() {
        when(deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);

        deviceCredentialsService.deleteDeviceCredentials(SYSTEM_TENANT_ID, createDummyDeviceCredentials(CREDENTIALS_ID_1, deviceId));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(2)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);
    }

    @Test
    public void testSaveDeviceCredentials_EvictsPreviousCache() throws Exception {
        when(deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);

        when(deviceCredentialsDao.findByDeviceId(SYSTEM_TENANT_ID, deviceId)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        UUID deviceCredentialsId = UUID.randomUUID();
        when(deviceCredentialsDao.findById(SYSTEM_TENANT_ID, deviceCredentialsId)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));
        when(deviceService.findDeviceById(SYSTEM_TENANT_ID, new DeviceId(deviceId))).thenReturn(new Device());

        deviceCredentialsService.updateDeviceCredentials(SYSTEM_TENANT_ID, createDummyDeviceCredentials(deviceCredentialsId, CREDENTIALS_ID_2, deviceId));

        when(deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1)).thenReturn(null);

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(3)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);
    }

    private DeviceCredentialsService unwrapDeviceCredentialsService() throws Exception {
        if (AopUtils.isAopProxy(deviceCredentialsService) && deviceCredentialsService instanceof Advised) {
            Object target = ((Advised) deviceCredentialsService).getTargetSource().getTarget();
            return (DeviceCredentialsService) target;
        }
        return null;
    }

    private DeviceCredentials createDummyDeviceCredentialsEntity(String deviceCredentialsId) {
        DeviceCredentials result = new DeviceCredentials(new DeviceCredentialsId(UUID.randomUUID()));
        result.setCredentialsId(deviceCredentialsId);
        return result;
    }

    private DeviceCredentials createDummyDeviceCredentials(String deviceCredentialsId, UUID deviceId) {
        return createDummyDeviceCredentials(null, deviceCredentialsId, deviceId);
    }

    private DeviceCredentials createDummyDeviceCredentials(UUID id, String deviceCredentialsId, UUID deviceId) {
        DeviceCredentials result = new DeviceCredentials();
        result.setId(new DeviceCredentialsId(id));
        result.setDeviceId(new DeviceId(deviceId));
        result.setCredentialsId(deviceCredentialsId);
        result.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        return result;
    }
}

