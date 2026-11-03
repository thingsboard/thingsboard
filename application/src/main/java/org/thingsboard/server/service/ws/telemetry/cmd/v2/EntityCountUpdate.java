// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

@ToString
public class EntityCountUpdate extends CmdUpdate {

    @Getter
    private int count;

    public EntityCountUpdate(int cmdId, int count) {
        super(cmdId, SubscriptionErrorCode.NO_ERROR.getCode(), null);
        this.count = count;
    }

    public EntityCountUpdate(int cmdId, int errorCode, String errorMsg) {
        super(cmdId, errorCode, errorMsg);
    }

    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.COUNT_DATA;
    }

    @JsonCreator
    public EntityCountUpdate(@JsonProperty("cmdId") int cmdId,
                             @JsonProperty("count") int count,
                             @JsonProperty("errorCode") int errorCode,
                             @JsonProperty("errorMsg") String errorMsg) {
        super(cmdId, errorCode, errorMsg);
        this.count = count;
    }

}
