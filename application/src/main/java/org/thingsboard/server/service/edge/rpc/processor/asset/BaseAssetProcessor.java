/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.asset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseAssetProcessor extends BaseEdgeProcessor {

    protected Pair<Boolean, Boolean> saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg) {
        boolean created = false;
        boolean assetNameUpdated = false;
        assetCreationLock.lock();
        try {
            Asset asset = constructAssetFromUpdateMsg(tenantId, assetId, assetUpdateMsg);
            if (asset == null) {
                throw new RuntimeException("[{" + tenantId + "}] assetUpdateMsg {" + assetUpdateMsg + " } cannot be converted to asset");
            }
            Asset assetById = assetService.findAssetById(tenantId, assetId);
            if (assetById == null) {
                created = true;
                asset.setId(null);
            } else {
                asset.setId(assetId);
            }
            String assetName = asset.getName();
            Asset assetByName = assetService.findAssetByTenantIdAndName(tenantId, assetName);
            if (assetByName != null && !assetByName.getId().equals(assetId)) {
                assetName = assetName + "_" + StringUtils.randomAlphanumeric(15);
                log.warn("[{}] Asset with name {} already exists. Renaming asset name to {}",
                        tenantId, asset.getName(), assetName);
                assetNameUpdated = true;
            }
            asset.setName(assetName);
            setCustomerId(tenantId, created ? null : assetById.getCustomerId(), asset, assetUpdateMsg);

            assetValidator.validate(asset, Asset::getTenantId);
            if (created) {
                asset.setId(assetId);
            }
            assetService.saveAsset(asset, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process asset update msg [{}]", tenantId, assetUpdateMsg, e);
            throw e;
        } finally {
            assetCreationLock.unlock();
        }
        return Pair.of(created, assetNameUpdated);
    }

    protected abstract Asset constructAssetFromUpdateMsg(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg);

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, Asset asset, AssetUpdateMsg assetUpdateMsg);

}
