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
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@Slf4j
@TbCoreComponent
public class OtaPackageEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertOtaPackageEventToDownlink(EdgeEvent edgeEvent) {
        OtaPackageId otaPackageId = new OtaPackageId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                OtaPackage otaPackage = otaPackageService.findOtaPackageById(edgeEvent.getTenantId(), otaPackageId);
                if (otaPackage != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    OtaPackageUpdateMsg otaPackageUpdateMsg =
                            otaPackageMsgConstructor.constructOtaPackageUpdatedMsg(msgType, otaPackage);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addOtaPackageUpdateMsg(otaPackageUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
                OtaPackageUpdateMsg otaPackageUpdateMsg =
                        otaPackageMsgConstructor.constructOtaPackageDeleteMsg(otaPackageId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addOtaPackageUpdateMsg(otaPackageUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processOtaPackageNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotificationForAllEdges(tenantId, edgeNotificationMsg);
    }
}
