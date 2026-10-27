// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceCredentialsEvictEvent {

    private final String newCredentialsId;
    private final String oldCredentialsId;
    private final boolean deleted;

    public DeviceCredentialsEvictEvent(String newCredentialsId, String oldCredentialsId) {
        this(newCredentialsId, oldCredentialsId, false);
    }

}
