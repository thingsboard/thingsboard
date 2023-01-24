/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Component
@Slf4j
@TbCoreComponent
public class AssetEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertAssetEventToDownlink(EdgeEvent edgeEvent) {
        AssetId assetId = new AssetId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Asset asset = assetService.findAssetById(edgeEvent.getTenantId(), assetId);
                if (asset != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AssetUpdateMsg assetUpdateMsg =
                            assetMsgConstructor.constructAssetUpdatedMsg(msgType, asset);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAssetUpdateMsg(assetUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        AssetProfile assetProfile = assetProfileService.findAssetProfileById(edgeEvent.getTenantId(), asset.getAssetProfileId());
                        builder.addAssetProfileUpdateMsg(assetProfileMsgConstructor.constructAssetProfileUpdatedMsg(msgType, assetProfile));
                    }
                    downlinkMsg = builder.build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                AssetUpdateMsg assetUpdateMsg =
                        assetMsgConstructor.constructAssetDeleteMsg(assetId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetUpdateMsg(assetUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processAssetNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotification(tenantId, edgeNotificationMsg);
    }
}
