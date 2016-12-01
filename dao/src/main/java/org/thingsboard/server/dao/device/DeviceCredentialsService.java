/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.dao.device;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CREDENTIALS_CACHE;

public interface DeviceCredentialsService {

    DeviceCredentials findDeviceCredentialsByDeviceId(DeviceId deviceId);

    @Cacheable(cacheNames = DEVICE_CREDENTIALS_CACHE, unless="#result == null")
    DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId);

    @CacheEvict(cacheNames = DEVICE_CREDENTIALS_CACHE, keyGenerator="previousDeviceCredentialsId", beforeInvocation = true)
    DeviceCredentials updateDeviceCredentials(DeviceCredentials deviceCredentials);

    DeviceCredentials createDeviceCredentials(DeviceCredentials deviceCredentials);

    @CacheEvict(cacheNames = DEVICE_CREDENTIALS_CACHE, key="#deviceCredentials.credentialsId")
    void deleteDeviceCredentials(DeviceCredentials deviceCredentials);
}
