/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

import java.util.List;

@ToString
public class AlarmDataUpdate extends DataUpdate<AlarmData> {

    @Getter
    private long allowedEntities;
    @Getter
    private long totalEntities;

    public AlarmDataUpdate(int cmdId, PageData<AlarmData> data, List<AlarmData> update, long allowedEntities, long totalEntities) {
        super(cmdId, data, update, SubscriptionErrorCode.NO_ERROR.getCode(), null);
        this.allowedEntities = allowedEntities;
        this.totalEntities = totalEntities;
    }

    public AlarmDataUpdate(int cmdId, int errorCode, String errorMsg) {
        super(cmdId, null, null, errorCode, errorMsg);
    }

    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.ALARM_DATA;
    }

    @JsonCreator
    public AlarmDataUpdate(@JsonProperty("cmdId") int cmdId,
                           @JsonProperty("data") PageData<AlarmData> data,
                           @JsonProperty("update") List<AlarmData> update,
                           @JsonProperty("errorCode") int errorCode,
                           @JsonProperty("errorMsg") String errorMsg,
                           @JsonProperty("allowedEntities") long allowedEntities,
                           @JsonProperty("totalEntities") long totalEntities) {
        super(cmdId, data, update, errorCode, errorMsg);
        this.allowedEntities = allowedEntities;
        this.totalEntities = totalEntities;
    }
}
