// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.TransportPayloadType;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "transportPayloadType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JsonTransportPayloadConfiguration.class, name = "JSON"),
        @JsonSubTypes.Type(value = ProtoTransportPayloadConfiguration.class, name = "PROTOBUF")})
public interface TransportPayloadTypeConfiguration extends Serializable {

    @JsonIgnore
    TransportPayloadType getTransportPayloadType();

}
