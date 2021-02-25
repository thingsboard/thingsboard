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
package org.thingsboard.server.dao.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.DeviceCredentialsService;

import java.lang.reflect.Method;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CREDENTIALS_CACHE;

@Component("previousDeviceCredentialsId")
public class PreviousDeviceCredentialsIdKeyGenerator implements KeyGenerator {

    private static final String NOT_VALID_DEVICE = DEVICE_CREDENTIALS_CACHE + "_notValidDeviceCredentialsId";

    @Override
    public Object generate(Object o, Method method, Object... objects) {
        DeviceCredentialsService deviceCredentialsService = (DeviceCredentialsService) o;
        TenantId tenantId = (TenantId) objects[0];
        DeviceCredentials deviceCredentials = (DeviceCredentials) objects[1];
        if (deviceCredentials.getDeviceId() != null) {
            DeviceCredentials oldDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceCredentials.getDeviceId());
            if (oldDeviceCredentials != null) {
                return DEVICE_CREDENTIALS_CACHE + "_" + oldDeviceCredentials.getCredentialsId();
            }
        }
        return NOT_VALID_DEVICE;
    }
}
