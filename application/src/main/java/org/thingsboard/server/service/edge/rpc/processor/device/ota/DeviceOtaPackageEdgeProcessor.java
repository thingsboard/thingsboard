// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.device.ota;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class DeviceOtaPackageEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DownlinkMsg downlinkMsg = null;
        try {
            DeviceGroupOtaPackage deviceGroupOtaPackage = JacksonUtil.convertValue(edgeEvent.getBody(), DeviceGroupOtaPackage.class);
            if (deviceGroupOtaPackage == null) {
                return null;
            }
            UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
            downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addDeviceGroupOtaPackageUpdateMsg(EdgeMsgConstructorUtils.constructDeviceGroupOtaUpdateMsg(msgType, deviceGroupOtaPackage))
                    .build();
        } catch (Exception e) {
            log.error("Can't process device group ota package msg [{}]", edgeEvent, e);
        }
        return downlinkMsg;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.DEVICE_GROUP_OTA;
    }

}
