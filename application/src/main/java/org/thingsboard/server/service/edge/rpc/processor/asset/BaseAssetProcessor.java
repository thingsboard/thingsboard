/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

import java.util.UUID;

@Slf4j
public abstract class BaseAssetProcessor extends BaseEdgeProcessor {

    protected Pair<Boolean, Boolean> saveOrUpdateAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg, EdgeVersion edgeVersion) {
        boolean created = false;
        boolean assetNameUpdated = false;
        assetCreationLock.lock();
        try {
            Asset asset = EdgeVersionUtils.isEdgeVersionOlderThan_3_6_2(edgeVersion)
                    ? createAsset(tenantId, assetId, assetUpdateMsg)
                    : JacksonUtil.fromStringIgnoreUnknownProperties(assetUpdateMsg.getEntity(), Asset.class);
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
            setCustomerId(tenantId, created ? null : assetById.getCustomerId(), asset, assetUpdateMsg, edgeVersion);

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

    private Asset createAsset(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(assetUpdateMsg.getName());
        asset.setCreatedTime(Uuids.unixTimestamp(assetId.getId()));
        asset.setType(assetUpdateMsg.getType());
        asset.setLabel(assetUpdateMsg.hasLabel() ? assetUpdateMsg.getLabel() : null);
        asset.setAdditionalInfo(assetUpdateMsg.hasAdditionalInfo()
                ? JacksonUtil.toJsonNode(assetUpdateMsg.getAdditionalInfo()) : null);

        UUID assetProfileUUID = safeGetUUID(assetUpdateMsg.getAssetProfileIdMSB(), assetUpdateMsg.getAssetProfileIdLSB());
        asset.setAssetProfileId(assetProfileUUID != null ? new AssetProfileId(assetProfileUUID) : null);

        CustomerId customerId = safeGetCustomerId(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB());
        asset.setCustomerId(customerId);
        return asset;
    }

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, Asset asset, AssetUpdateMsg assetUpdateMsg, EdgeVersion edgeVersion);
}
