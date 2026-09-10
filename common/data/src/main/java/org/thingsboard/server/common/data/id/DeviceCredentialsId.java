// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class DeviceCredentialsId extends UUIDBased {

    @JsonCreator
    public DeviceCredentialsId(@JsonProperty("id") UUID id) {
        super(id);
    }
}
