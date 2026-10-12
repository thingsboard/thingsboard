// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema
public class DeviceCertificateRevocationId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public DeviceCertificateRevocationId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static DeviceCertificateRevocationId fromString(String revocationId) {
        return new DeviceCertificateRevocationId(UUID.fromString(revocationId));
    }

}
