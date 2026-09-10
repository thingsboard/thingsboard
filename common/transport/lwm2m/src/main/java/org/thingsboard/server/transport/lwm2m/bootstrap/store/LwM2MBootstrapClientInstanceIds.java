// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class LwM2MBootstrapClientInstanceIds {

    /**
     * Map<serverId (shortId), InstanceId>
     */
    private Map<Integer, Integer> securityInstances = new HashMap<>();
    private  Map<Integer, Integer> serverInstances = new HashMap<>();
}
