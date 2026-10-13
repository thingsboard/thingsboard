// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.data;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;

@Data
@Builder
public class UplinkData {

    private final String deviceName;
    private final String deviceType;
    private final String deviceLabel;
    private final String assetName;
    private final String assetType;
    private final String assetLabel;
    private final String customerName;
    private final String groupName;
    private final PostTelemetryMsg telemetry;
    private final PostAttributeMsg attributesUpdate;
    private final PostAttributeMsg constants;
    private final boolean isAsset;

}
