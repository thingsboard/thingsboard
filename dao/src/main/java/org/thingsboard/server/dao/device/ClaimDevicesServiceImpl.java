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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.claim.ClaimData;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.common.data.CacheConstants.CLAIM_DEVICES_CACHE;

@Service
@Slf4j
public class ClaimDevicesServiceImpl implements ClaimDevicesService {

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private AttributesService attributesService;
    @Autowired
    private CacheManager cacheManager;

    @Value("${device.claim.duration}")
    private long systemDurationMs;

    //TODO: @dlandiak rename to registerClaimingInfo
    @Override
    public ListenableFuture<Void> claimDevice(TenantId tenantId, DeviceId deviceId, String secretKey, long durationMs) {
        ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(tenantId, deviceId);
        return Futures.transformAsync(deviceFuture, device -> {
            Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);
            List<Object> key = new ArrayList<>();
            key.add(device.getName());
            key.add(secretKey);

            ListenableFuture<List<AttributeKvEntry>> claimingAllowedFuture = attributesService.find(tenantId, device.getId(),
                    DataConstants.SERVER_SCOPE, Collections.singletonList(ModelConstants.CLAIM_ATTRIBUTE_NAME));
            return Futures.transform(claimingAllowedFuture, list -> {
                if (list != null && !list.isEmpty()) {
                    Optional<Boolean> claimingAllowedOptional = list.get(0).getBooleanValue();
                    if (claimingAllowedOptional.isPresent() && claimingAllowedOptional.get()
                            && device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                        ClaimData claimData = new ClaimData(secretKey,
                                System.currentTimeMillis() + validateDurationMs(durationMs));
                        cache.putIfAbsent(key, claimData);
                        return null;
                    }
                }
                log.warn("The device [{}] has been already claimed!", device.getName());
                throw new IllegalArgumentException();
            });

        });
    }

    private long validateDurationMs(long durationMs) {
        if (durationMs > 0L) {
            return durationMs;
        }
        return systemDurationMs;
    }

    //TODO: @dlandiak rename to claimDevice
    @Override
    public ListenableFuture<ClaimResponse> processClaimDevice(Device device, CustomerId customerId, String secretKey) {
        List<Object> key = new ArrayList<>();
        //TODO: @dlandiak add special symbols
        key.add(device.getName());
        key.add(secretKey);

        Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);

        ClaimData claimData = cache.get(key, ClaimData.class);
        if (claimData != null) {
            long currTs = System.currentTimeMillis();
            if (currTs > claimData.getExpirationTime() || !secretKey.equals(claimData.getSecretKey())) {
                log.debug("The claiming timeout occurred for the device [{}]", device.getName());
                //TODO: @dlandiak cache evict
                return Futures.immediateFuture(ClaimResponse.TIMEOUT);
            } else {
                //TODO: @dlandiak check that device is not already assigned to customer at this stage
                device.setCustomerId(customerId);
                deviceService.saveDevice(device);

                cache.evict(key);

                ListenableFuture<List<Void>> future = attributesService.removeAll(device.getTenantId(),
                        device.getId(), DataConstants.SERVER_SCOPE, Collections.singletonList(ModelConstants.CLAIM_ATTRIBUTE_NAME));
                return Futures.transform(future, result -> ClaimResponse.SUCCESS);
            }
        } else {
            log.debug("Failed to find the device's claiming message![{}]", device.getName());
            return Futures.immediateFuture(ClaimResponse.CLAIMED);
        }
    }

    @Override
    public ListenableFuture<List<Void>> reClaimDevice(TenantId tenantId, Device device) {
        if (!device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            //TODO: @dlandiak remove info from cache.
            device.setCustomerId(null);
            deviceService.saveDevice(device);
            return attributesService.save(tenantId, device.getId(), DataConstants.SERVER_SCOPE, Collections.singletonList(
                    new BaseAttributeKvEntry(new BooleanDataEntry(ModelConstants.CLAIM_ATTRIBUTE_NAME, true),
                            System.currentTimeMillis())));
        }
        return Futures.immediateFuture(null);
    }
}
