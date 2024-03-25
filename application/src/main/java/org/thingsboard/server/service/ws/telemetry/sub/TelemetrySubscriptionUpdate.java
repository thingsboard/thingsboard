/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.ws.telemetry.sub;

import lombok.AllArgsConstructor;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@AllArgsConstructor
public class TelemetrySubscriptionUpdate {
    private final int subscriptionId;
    private int errorCode;
    private String errorMsg;
    private Map<String, List<Object>> data;

    public TelemetrySubscriptionUpdate(int subscriptionId, List<TsKvEntry> data) {
        super();
        this.subscriptionId = subscriptionId;
        this.data = new TreeMap<>();
        if (data != null) {
            for (TsKvEntry tsEntry : data) {
                List<Object> values = this.data.computeIfAbsent(tsEntry.getKey(), k -> new ArrayList<>());
                Object[] value = new Object[2];
                value[0] = tsEntry.getTs();
                value[1] = tsEntry.getValueAsString();
                values.add(value);
            }
        }
    }

    public TelemetrySubscriptionUpdate(int subscriptionId, Map<String, List<Object>> data) {
        super();
        this.subscriptionId = subscriptionId;
        this.data = data;
    }

    public TelemetrySubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode) {
        this(subscriptionId, errorCode, null);
    }

    public TelemetrySubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode, String errorMsg) {
        super();
        this.subscriptionId = subscriptionId;
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorMsg != null ? errorMsg : errorCode.getDefaultMsg();
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public Map<String, List<Object>> getData() {
        return data;
    }

    public Map<String, Long> getLatestValues() {
        if (data == null) {
            return Collections.emptyMap();
        } else {
            return data.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                List<Object> data = e.getValue();
                Object[] latest = (Object[]) data.get(data.size() - 1);
                return (long) latest[0];
            }));
        }
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public TelemetrySubscriptionUpdate copyWithNewSubscriptionId(int subscriptionId){
        return new TelemetrySubscriptionUpdate(subscriptionId, errorCode, errorMsg, data);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("TelemetrySubscriptionUpdate [subscriptionId=" + subscriptionId + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", data=");
        data.forEach((k, v) -> {
            result.append(k).append("=[");
            for(Object a : v){
                result.append(Arrays.toString((Object[])a)).append("|");
            }
            result.append("]");
        });
        return result.toString();
    }
}
