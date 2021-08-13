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
package org.thingsboard.server.service.asset;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.importing.AbstractBulkImportService;
import org.thingsboard.server.service.importing.BulkImportColumnType;
import org.thingsboard.server.service.importing.BulkImportRequest;
import org.thingsboard.server.service.importing.ImportedEntityInfo;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Map;
import java.util.Optional;

@Service
@TbCoreComponent
public class AssetBulkImportService extends AbstractBulkImportService<Asset> {
    private final AssetService assetService;

    public AssetBulkImportService(TelemetrySubscriptionService tsSubscriptionService, TbTenantProfileCache tenantProfileCache,
                                  AccessControlService accessControlService, AccessValidator accessValidator,
                                  EntityActionService entityActionService, TbClusterService clusterService, AssetService assetService) {
        super(tsSubscriptionService, tenantProfileCache, accessControlService, accessValidator, entityActionService, clusterService);
        this.assetService = assetService;
    }

    @Override
    protected ImportedEntityInfo<Asset> saveEntity(BulkImportRequest importRequest, Map<BulkImportColumnType, String> fields, SecurityUser user) {
        ImportedEntityInfo<Asset> importedEntityInfo = new ImportedEntityInfo<>();

        Asset asset = new Asset();
        asset.setTenantId(user.getTenantId());
        setAssetFields(asset, fields);

        Asset existingAsset = assetService.findAssetByTenantIdAndName(user.getTenantId(), asset.getName());
        if (existingAsset != null && importRequest.getMapping().getUpdate()) {
            importedEntityInfo.setOldEntity(new Asset(existingAsset));
            importedEntityInfo.setUpdated(true);
            existingAsset.update(asset);
            asset = existingAsset;
        }
        asset = assetService.saveAsset(asset);

        importedEntityInfo.setEntity(asset);
        return importedEntityInfo;
    }

    private void setAssetFields(Asset asset, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = (ObjectNode) Optional.ofNullable(asset.getAdditionalInfo()).orElseGet(JacksonUtil::newObjectNode);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    asset.setName(value);
                    break;
                case TYPE:
                    asset.setType(value);
                    break;
                case LABEL:
                    asset.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
            }
        });
        asset.setAdditionalInfo(additionalInfo);
    }

}
