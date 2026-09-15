// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.service.ws.WsCmdType;

public class AlarmCountCmd extends DataCmd {

    @Getter
    private final AlarmCountQuery query;

    @JsonCreator
    public AlarmCountCmd(@JsonProperty("cmdId") int cmdId,
                         @JsonProperty("query") AlarmCountQuery query) {
        super(cmdId);
        this.query = query;
    }

    @Override
    public WsCmdType getType() {
        return WsCmdType.ALARM_COUNT;
    }
}
