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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.claimdata.ClaimData;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.annotation.Nullable;
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
    private DeviceCredentialsService deviceCredentialsService;
    @Autowired
    private AttributesService attributesService;
    @Autowired
    private CacheManager cacheManager;

    public ListenableFuture<Boolean> claimDevice(TenantId tenantId, CustomerId customerId, Device device, String secretKey, long durationMs) {
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        String accessToken = deviceCredentials.getCredentialsId();

        Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);
        List<Object> key = new ArrayList<>();
        key.add(accessToken);
        key.add(secretKey);

        ListenableFuture<List<AttributeKvEntry>> claimingAllowedFuture = attributesService.find(tenantId, device.getId(),
                DataConstants.SERVER_SCOPE, Collections.singletonList(ModelConstants.CLAIM_ATTRIBUTE_NAME));
        return Futures.transform(claimingAllowedFuture, list -> {
            if (list != null && !list.isEmpty()) {
                Optional<Boolean> claimingAllowedOptional = list.get(0).getBooleanValue();
                if (claimingAllowedOptional.isPresent() && claimingAllowedOptional.get()
                        && device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                    ClaimData claimData = new ClaimData(tenantId, customerId, device.getName(), secretKey,
                            System.currentTimeMillis() + durationMs);
                    cache.putIfAbsent(key, claimData);
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public void processClaimDevice(String accessToken, String secretKey) {
        List<Object> key = new ArrayList<>();
        key.add(accessToken);
        key.add(secretKey);

        Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);

        ClaimData claimData = cache.get(key, ClaimData.class);
        if (claimData != null) {
            long currTs = System.currentTimeMillis();
            if (currTs > claimData.getExpirationTime() || !secretKey.equals(claimData.getSecretKey())) {
                log.debug("Wrong secret key specified to claim device or claiming timeout occurred!");
                throw new IllegalStateException();
            } else {
                Device device = deviceService.findDeviceByTenantIdAndName(claimData.getTenantId(), claimData.getDeviceName());
                device.setCustomerId(claimData.getCustomerId());
                deviceService.saveDevice(device);

                cache.evict(key);

                ListenableFuture<List<Void>> future = attributesService.removeAll(claimData.getTenantId(),
                        device.getId(), DataConstants.SERVER_SCOPE, Collections.singletonList(ModelConstants.CLAIM_ATTRIBUTE_NAME));
                Futures.addCallback(future, new FutureCallback<List<Void>>() {
                    @Override
                    public void onSuccess(@Nullable List<Void> result) {
                        log.debug("Claim attribute has been removed successfully!");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("Failed to remove claim device attribute!", t);
                    }
                });
            }
        } else {
            log.debug("The device firstly needs to be claimed on the UI!");
            throw new IllegalStateException();
        }
    }
}
