// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip.configs;

import lombok.Data;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.tcpip.AbstractIpIntegration;
import org.thingsboard.integration.tcpip.HandlerConfiguration;

@Data
public class JsonHandlerConfiguration implements HandlerConfiguration {

    @Override
    public String getHandlerType() {
        return AbstractIpIntegration.JSON_PAYLOAD;
    }

    @Override
    public ContentType getUplinkContentType() {return ContentType.JSON;}

}
