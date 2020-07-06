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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;

import java.util.List;

@Data
@AllArgsConstructor
public abstract class DataUpdate<T> {

    private final int cmdId;
    private final PageData<T> data;
    private final List<T> update;
    private final int errorCode;
    private final String errorMsg;

    public DataUpdate(int cmdId, PageData<T> data, List<T> update) {
        this(cmdId, data, update, SubscriptionErrorCode.NO_ERROR.getCode(), null);
    }

    public DataUpdate(int cmdId, int errorCode, String errorMsg) {
        this(cmdId, null, null, errorCode, errorMsg);
    }

    public abstract DataUpdateType getDataUpdateType();
}
