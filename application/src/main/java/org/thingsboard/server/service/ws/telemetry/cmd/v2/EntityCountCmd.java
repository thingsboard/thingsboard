// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.service.ws.WsCmdType;

public class EntityCountCmd extends DataCmd {

    @Getter
    private final EntityCountQuery query;

    @JsonCreator
    public EntityCountCmd(@JsonProperty("cmdId") int cmdId,
                          @JsonProperty("query") EntityCountQuery query) {
        super(cmdId);
        this.query = query;
    }

    @Override
    public WsCmdType getType() {
        return WsCmdType.ENTITY_COUNT;
    }
}
