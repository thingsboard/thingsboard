// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data.cmd;

import lombok.Data;
import org.thingsboard.server.common.data.query.EntityDataQuery;

// hand-rolled mirror of application's org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd
@Data
public class EntityDataCmd {

    // must match the server's @JsonSubTypes discriminator for this command - not compile-checked against it
    public static final String TYPE = "ENTITY_DATA";

    private int cmdId;
    private EntityDataQuery query;
    private LatestValueCmd latestCmd;
    private final String type = TYPE;

}
