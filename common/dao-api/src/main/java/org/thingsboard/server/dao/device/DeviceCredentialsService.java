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
package org.thingsboard.server.dao.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.ServerSecurityConfig;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;

public interface DeviceCredentialsService {

    DeviceCredentials findDeviceCredentialsByDeviceId(TenantId tenantId, DeviceId deviceId);

    DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId);

    DeviceCredentials updateDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    DeviceCredentials createDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    void deleteDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    void verifySecurityKeyDevice(DeviceCredentials deviceCredentials) throws JsonProcessingException, InvalidConfigurationException;

    void verifySecurityKeyDeviceProfile(DeviceProfile deviceProfile) throws InvalidConfigurationException, JsonProcessingException;

    ServerSecurityConfig getServerSecurityInfo(boolean bootstrapServer);
}
