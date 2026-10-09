// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip.tcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.integration.tcpip.HandlerConfiguration;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class TcpConfigurationParameters {
    private int port;
    private int soBacklogOption;
    private int soRcvBuf;
    private int soSndBuf;
    private boolean soKeepaliveOption;
    private boolean tcpNoDelay;
    private JsonNode metadata;
    private HandlerConfiguration handlerConfiguration;
}
