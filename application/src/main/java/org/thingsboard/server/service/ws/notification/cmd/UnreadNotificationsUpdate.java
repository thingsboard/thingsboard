// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.notification.cmd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdateType;

import java.util.List;

@Getter
@ToString(exclude = "notifications")
public class UnreadNotificationsUpdate extends CmdUpdate {

    private final List<Notification> notifications;
    private final Notification update;
    private final int totalUnreadCount;
    private final int sequenceNumber;

    @Builder
    @JsonCreator
    public UnreadNotificationsUpdate(@JsonProperty("cmdId") int cmdId, @JsonProperty("errorCode") int errorCode,
                                     @JsonProperty("errorMsg") String errorMsg,
                                     @JsonProperty("notifications") List<Notification> notifications,
                                     @JsonProperty("update") Notification update,
                                     @JsonProperty("totalUnreadCount") int totalUnreadCount,
                                     @JsonProperty("sequenceNumber") int sequenceNumber) {
        super(cmdId, errorCode, errorMsg);
        this.notifications = notifications;
        this.update = update;
        this.totalUnreadCount = totalUnreadCount;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.NOTIFICATIONS;
    }

}
