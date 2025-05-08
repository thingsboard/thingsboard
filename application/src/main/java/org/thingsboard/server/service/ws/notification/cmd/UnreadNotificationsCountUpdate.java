/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
