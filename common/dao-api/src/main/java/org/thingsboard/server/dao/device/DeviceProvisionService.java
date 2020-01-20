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
package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.ProvisionProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.provision.ProvisionProfile;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;

public interface DeviceProvisionService {

    ProvisionProfile findProfileById(TenantId tenantId, ProvisionProfileId profileId);

    ListenableFuture<ProvisionProfile> findProfileByIdAsync(TenantId tenantId, ProvisionProfileId profileId);

    ProvisionProfile saveProvisionProfile(ProvisionProfile provisionProfile);

    ProvisionProfile findProvisionProfileByKeyAndSecret(TenantId tenantId, String key, String secret);

    void deleteProfile(TenantId tenantId, ProvisionProfileId profileId);

    ListenableFuture<ProvisionResponse> provisionDevice(ProvisionRequest provisionRequest);

}
