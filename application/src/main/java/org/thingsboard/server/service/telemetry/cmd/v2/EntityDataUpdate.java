/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;

import java.util.List;

public class EntityDataUpdate extends DataUpdate<EntityData> {

    public EntityDataUpdate(int cmdId, PageData<EntityData> data, List<EntityData> update) {
        super(cmdId, data, update, SubscriptionErrorCode.NO_ERROR.getCode(), null);
    }

    public EntityDataUpdate(int cmdId, int errorCode, String errorMsg) {
        super(cmdId, null, null, errorCode, errorMsg);
    }

    @Override
    public DataUpdateType getDataUpdateType() {
        return DataUpdateType.ENTITY_DATA;
    }

    @JsonCreator
    public EntityDataUpdate(@JsonProperty("cmdId") int cmdId,
                            @JsonProperty("data") PageData<EntityData> data,
                            @JsonProperty("update") List<EntityData> update,
                            @JsonProperty("errorCode") int errorCode,
                            @JsonProperty("errorMsg") String errorMsg) {
        super(cmdId, data, update, errorCode, errorMsg);
    }

}
