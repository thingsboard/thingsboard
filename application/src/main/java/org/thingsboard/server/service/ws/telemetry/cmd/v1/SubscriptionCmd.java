// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public abstract class SubscriptionCmd implements TelemetryPluginCmd {

    private int cmdId;
    private String entityType;
    private String entityId;
    private String keys;
    private String scope;
    private boolean unsubscribe;

    @Override
    public String toString() {
        return "SubscriptionCmd [entityType=" + entityType  + ", entityId=" + entityId + ", tags=" + keys + ", unsubscribe=" + unsubscribe + "]";
    }

}
