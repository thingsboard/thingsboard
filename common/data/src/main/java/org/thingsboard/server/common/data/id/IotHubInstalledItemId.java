// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class IotHubInstalledItemId extends UUIDBased {

    @JsonCreator
    public IotHubInstalledItemId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static IotHubInstalledItemId fromString(String id) {
        return new IotHubInstalledItemId(UUID.fromString(id));
    }

}
