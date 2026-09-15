// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification.cmd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdateType;

@Getter
@ToString
public class UnreadNotificationsCountUpdate extends CmdUpdate {

    private final int totalUnreadCount;
    private final int sequenceNumber;

    @Builder
    @JsonCreator
    public UnreadNotificationsCountUpdate(@JsonProperty("cmdId") int cmdId, @JsonProperty("errorCode") int errorCode,
                                          @JsonProperty("errorMsg") String errorMsg,
                                          @JsonProperty("totalUnreadCount") int totalUnreadCount,
                                          @JsonProperty("sequenceNumber") int sequenceNumber) {
        super(cmdId, errorCode, errorMsg);
        this.totalUnreadCount = totalUnreadCount;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.NOTIFICATIONS_COUNT;
    }

}
