/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.cf;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.cf.cache.CalculatedFieldEntityProfileCache;

@Slf4j
@Service
@TbRuleEngineComponent
@RequiredArgsConstructor
public class DefaultCalculatedFieldInitService implements CalculatedFieldInitService {

    private final CalculatedFieldEntityProfileCache entityProfileCache;
    private final AssetService assetService;
    private final DeviceService deviceService;

    @Value("${calculated_fields.init_fetch_pack_size:50000}")
    @Getter
    private int initFetchPackSize;

    @AfterStartUp(order = AfterStartUp.CF_READ_PROFILE_ENTITIES_SERVICE)
    public void initCalculatedFieldDefinitions() {
        PageDataIterable<ProfileEntityIdInfo> deviceIdInfos = new PageDataIterable<>(deviceService::findProfileEntityIdInfos, initFetchPackSize);
        for (ProfileEntityIdInfo idInfo : deviceIdInfos) {
            log.trace("Processing device record: {}", idInfo);
            try {
                entityProfileCache.add(idInfo.getTenantId(), idInfo.getProfileId(), idInfo.getEntityId());
            } catch (Exception e) {
                log.error("Failed to process device record: {}", idInfo, e);
            }
        }
        PageDataIterable<ProfileEntityIdInfo> assetIdInfos = new PageDataIterable<>(assetService::findProfileEntityIdInfos, initFetchPackSize);
        for (ProfileEntityIdInfo idInfo : assetIdInfos) {
            log.trace("Processing asset record: {}", idInfo);
            try {
                entityProfileCache.add(idInfo.getTenantId(), idInfo.getProfileId(), idInfo.getEntityId());
            } catch (Exception e) {
                log.error("Failed to process asset record: {}", idInfo, e);
            }
        }
    }

}
