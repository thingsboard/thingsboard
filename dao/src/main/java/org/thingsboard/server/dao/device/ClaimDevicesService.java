/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.claim.ClaimResult;

import java.util.List;

public interface ClaimDevicesService {

    ListenableFuture<Void> registerClaimingInfo(TenantId tenantId, DeviceId deviceId, String secretKey, long durationMs);

    ListenableFuture<ClaimResult> claimDevice(Device device, CustomerId customerId, String secretKey);

    ListenableFuture<List<Void>> reClaimDevice(TenantId tenantId, Device device);

}
