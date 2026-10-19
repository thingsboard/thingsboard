// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.TransportPayloadType;

@Data
@Schema
public class JsonTransportPayloadConfiguration implements TransportPayloadTypeConfiguration {

    @Override
    public TransportPayloadType getTransportPayloadType() {
        return TransportPayloadType.JSON;
    }
}
