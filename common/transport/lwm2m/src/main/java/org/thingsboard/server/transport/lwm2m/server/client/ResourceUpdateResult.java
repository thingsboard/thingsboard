// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
public class ResourceUpdateResult {
    private LwM2mClient lwM2MClient;
    private Set<String> paths;

    public ResourceUpdateResult(LwM2mClient lwM2MClient) {
        this.lwM2MClient = lwM2MClient;
        this.paths = new HashSet<>();
    }
}
