// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.List;

public class EdgeMsgSummaryUtilsTest {

    private static final String SECRET = "secret-access-token";

    @Test
    public void testDownlinkMsgSummaryHasNoPayload() {
        DownlinkMsg downlinkMsg = DownlinkMsg.newBuilder()
                .setDownlinkMsgId(12)
                .addDeviceUpdateMsg(DeviceUpdateMsg.newBuilder().setEntity("{\"name\":\"Device\"}").build())
                .addDeviceUpdateMsg(DeviceUpdateMsg.newBuilder().setEntity("{\"name\":\"Device 2\"}").build())
                .addDeviceCredentialsUpdateMsg(DeviceCredentialsUpdateMsg.newBuilder().setEntity("{\"credentialsId\":\"" + SECRET + "\"}").build())
                .build();

        String summary = EdgeMsgSummaryUtils.toSummary(downlinkMsg);

        Assertions.assertEquals("DownlinkMsg[downlinkMsgId=12, deviceUpdateMsg x2, deviceCredentialsUpdateMsg x1]", summary);
    }

    @Test
    public void testUplinkMsgsSummaryHasNoPayload() {
        UplinkMsg uplinkMsg = UplinkMsg.newBuilder()
                .setUplinkMsgId(3)
                .addDeviceCredentialsUpdateMsg(DeviceCredentialsUpdateMsg.newBuilder().setEntity("{\"credentialsId\":\"" + SECRET + "\"}").build())
                .build();

        String summary = EdgeMsgSummaryUtils.toSummary(List.of(uplinkMsg, uplinkMsg));

        Assertions.assertEquals("[UplinkMsg[uplinkMsgId=3, deviceCredentialsUpdateMsg x1], UplinkMsg[uplinkMsgId=3, deviceCredentialsUpdateMsg x1]]", summary);
        Assertions.assertFalse(summary.contains(SECRET));
    }

}
