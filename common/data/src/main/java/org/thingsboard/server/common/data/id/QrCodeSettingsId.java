// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema
public class QrCodeSettingsId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public QrCodeSettingsId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static QrCodeSettingsId fromString(String qrCodeSettingsId) {
        return new QrCodeSettingsId(UUID.fromString(qrCodeSettingsId));
    }
}
