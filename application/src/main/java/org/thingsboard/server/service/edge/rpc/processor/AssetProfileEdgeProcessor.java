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
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@Slf4j
@TbCoreComponent
public class AssetProfileEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertAssetProfileEventToDownlink(EdgeEvent edgeEvent) {
        AssetProfileId assetProfileId = new AssetProfileId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                AssetProfile assetProfile = assetProfileService.findAssetProfileById(edgeEvent.getTenantId(), assetProfileId);
                if (assetProfile != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    AssetProfileUpdateMsg assetProfileUpdateMsg =
                            assetProfileMsgConstructor.constructAssetProfileUpdatedMsg(msgType, assetProfile);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAssetProfileUpdateMsg(assetProfileUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
                AssetProfileUpdateMsg assetProfileUpdateMsg =
                        assetProfileMsgConstructor.constructAssetProfileDeleteMsg(assetProfileId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addAssetProfileUpdateMsg(assetProfileUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processAssetProfileNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotificationForAllEdges(tenantId, edgeNotificationMsg);
    }

}
