// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip.configs;

import lombok.Data;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.tcpip.AbstractIpIntegration;
import org.thingsboard.integration.tcpip.HandlerConfiguration;

@Data
public class BinaryHandlerConfiguration implements HandlerConfiguration {

    private String byteOrder;
    private int maxFrameLength;
    private int lengthFieldOffset;
    private int lengthFieldLength;
    private int lengthAdjustment;
    private int initialBytesToStrip;
    private boolean failFast;

    @Override
    public String getHandlerType() {
        return AbstractIpIntegration.BINARY_PAYLOAD;
    }

    @Override
    public ContentType getUplinkContentType() {return ContentType.BINARY;}

}
