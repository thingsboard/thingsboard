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
package org.thingsboard.server.common.transport.service;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportProfileCache;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true') || '${service.type:null}'=='tb-transport'")
public class DefaultTransportProfileCache implements TransportProfileCache {

    private final ConcurrentMap<DeviceProfileId, DeviceProfile> deviceProfiles = new ConcurrentHashMap<>();

    private final DataDecodingEncodingService dataDecodingEncodingService;

    public DefaultTransportProfileCache(DataDecodingEncodingService dataDecodingEncodingService) {
        this.dataDecodingEncodingService = dataDecodingEncodingService;
    }

    @Override
    public DeviceProfile getOrCreate(DeviceProfileId id, ByteString profileBody) {
        DeviceProfile profile = deviceProfiles.get(id);
        if (profile == null) {
            Optional<DeviceProfile> deviceProfile = dataDecodingEncodingService.decode(profileBody.toByteArray());
            if (deviceProfile.isPresent()) {
                profile = deviceProfile.get();
                deviceProfiles.put(id, profile);
            }
        }
        return profile;
    }

    @Override
    public DeviceProfile get(DeviceProfileId id) {
        return deviceProfiles.get(id);
    }

    @Override
    public void put(DeviceProfile profile) {
        deviceProfiles.put(profile.getId(), profile);
    }

    @Override
    public DeviceProfile put(ByteString profileBody) {
        Optional<DeviceProfile> deviceProfile = dataDecodingEncodingService.decode(profileBody.toByteArray());
        if (deviceProfile.isPresent()) {
            put(deviceProfile.get());
            return deviceProfile.get();
        } else {
            return null;
        }
    }

    @Override
    public void evict(DeviceProfileId id) {
        deviceProfiles.remove(id);
    }
}
