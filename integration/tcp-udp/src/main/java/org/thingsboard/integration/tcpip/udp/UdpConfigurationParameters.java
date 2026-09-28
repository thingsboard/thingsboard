// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip.udp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.integration.tcpip.HandlerConfiguration;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class UdpConfigurationParameters {
    private int port;
    private int soRcvBuf;
    private boolean soBroadcast;
    private JsonNode metadata;
    private HandlerConfiguration handlerConfiguration;
}