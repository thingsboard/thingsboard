// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tcpip;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.tcpip.configs.BinaryHandlerConfiguration;
import org.thingsboard.integration.tcpip.configs.HexHandlerConfiguration;
import org.thingsboard.integration.tcpip.configs.JsonHandlerConfiguration;
import org.thingsboard.integration.tcpip.configs.TextHandlerConfiguration;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, property = "handlerType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextHandlerConfiguration.class, name = "TEXT"),
        @JsonSubTypes.Type(value = BinaryHandlerConfiguration.class, name = "BINARY"),
        @JsonSubTypes.Type(value = JsonHandlerConfiguration.class, name = "JSON"),
        @JsonSubTypes.Type(value = HexHandlerConfiguration.class, name = "HEX")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface HandlerConfiguration {

    String getHandlerType();

    ContentType getUplinkContentType();

}