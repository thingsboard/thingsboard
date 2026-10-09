// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip.configs;

import lombok.Data;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.tcpip.AbstractIpIntegration;
import org.thingsboard.integration.tcpip.HandlerConfiguration;

@Data
public class TextHandlerConfiguration implements HandlerConfiguration {

    private int maxFrameLength;
    private boolean stripDelimiter;
    private TextMessageSeparatorType messageSeparator;
    private String customSeparatorRawValue;
    private String charsetName;

    @Override
    public String getHandlerType() {
        return AbstractIpIntegration.TEXT_PAYLOAD;
    }

    @Override
    public ContentType getUplinkContentType() {
        return ContentType.TEXT;
    }

}
