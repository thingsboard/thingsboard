// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

@ToString
@Getter
public class AlarmStatusUpdate extends CmdUpdate {

    @Getter
    private boolean active;

    public AlarmStatusUpdate(int cmdId, boolean active) {
        super(cmdId, SubscriptionErrorCode.NO_ERROR.getCode(), null);
        this.active = active;
    }

    public AlarmStatusUpdate(int cmdId, int errorCode, String errorMsg) {
        super(cmdId, errorCode, errorMsg);
    }

    @Builder
    public AlarmStatusUpdate(@JsonProperty("cmdId") int cmdId,
                             @JsonProperty("present") boolean active,
                             @JsonProperty("errorCode") int errorCode,
                             @JsonProperty("errorMsg") String errorMsg) {
        super(cmdId, errorCode, errorMsg);
        this.active = active;
    }

    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.ALARM_STATUS;
    }

}
