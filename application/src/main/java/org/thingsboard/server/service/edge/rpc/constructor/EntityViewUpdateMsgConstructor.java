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
package org.thingsboard.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.gen.edge.EdgeEntityType;
import org.thingsboard.server.gen.edge.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;

@Component
@Slf4j
public class EntityViewUpdateMsgConstructor {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    public EntityViewUpdateMsg constructEntityViewUpdatedMsg(UpdateMsgType msgType, EntityView entityView) {
        String relatedName;
        String relatedType;
        EdgeEntityType relatedEntityType;
        if (entityView.getEntityId().getEntityType().equals(EntityType.DEVICE)) {
            Device device = deviceService.findDeviceById(entityView.getTenantId(), new DeviceId(entityView.getEntityId().getId()));
            relatedName = device.getName();
            relatedType = device.getType();
            relatedEntityType = EdgeEntityType.DEVICE;
        } else {
            Asset asset = assetService.findAssetById(entityView.getTenantId(), new AssetId(entityView.getEntityId().getId()));
            relatedName = asset.getName();
            relatedType = asset.getType();
            relatedEntityType = EdgeEntityType.ASSET;
        }
        EntityViewUpdateMsg.Builder builder = EntityViewUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setName(entityView.getName())
                .setType(entityView.getType())
                .setRelatedName(relatedName)
                .setRelatedType(relatedType)
                .setRelatedEntityType(relatedEntityType);
        return builder.build();
    }

}
