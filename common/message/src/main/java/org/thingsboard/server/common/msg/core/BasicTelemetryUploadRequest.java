/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionMsgType;

public class BasicTelemetryUploadRequest extends BasicRequest implements TelemetryUploadRequest {

    private static final long serialVersionUID = 1L;

    private final Map<Long, List<KvEntry>> data;

    public BasicTelemetryUploadRequest() {
        this(DEFAULT_REQUEST_ID);
    }

    public BasicTelemetryUploadRequest(Integer requestId) {
        super(requestId);
        this.data = new HashMap<>();
    }

    public void add(long ts, KvEntry entry) {
        List<KvEntry> tsEntries = data.get(ts);
        if (tsEntries == null) {
            tsEntries = new ArrayList<>();
            data.put(ts, tsEntries);
        }
        tsEntries.add(entry);
    }

    @Override
    public SessionMsgType getMsgType() {
        return SessionMsgType.POST_TELEMETRY_REQUEST;
    }

    @Override
    public Map<Long, List<KvEntry>> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "BasicTelemetryUploadRequest [data=" + data + "]";
    }

}
