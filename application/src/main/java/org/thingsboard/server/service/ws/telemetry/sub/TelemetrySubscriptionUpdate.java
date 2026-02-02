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
package org.thingsboard.server.service.ws.telemetry.sub;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class TelemetrySubscriptionUpdate {

    @With
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

    @JsonIgnore
    public Map<String, List<TsValue>> getValues() {
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<TsValue>> data = new HashMap<>();
        this.data.forEach((key, entries) -> {
            if (entries.isEmpty()) {
                return;
            }

            List<TsValue> values = new ArrayList<>(entries.size());
            entries.forEach(object -> {
                if (!(object instanceof Object[] entry) || entry.length < 2) {
                    return;
                }
                TsValue tsValue = new TsValue((Long) entry[0], (String) entry[1]);
                values.add(tsValue);
            });
            data.put(key, values);
        });
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

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("TelemetrySubscriptionUpdate [subscriptionId=" + subscriptionId + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg + ", data=");
        data.forEach((k, v) -> {
            result.append(k).append("=[");
            for (Object a : v) {
                result.append(Arrays.toString((Object[]) a)).append("|");
            }
            result.append("]");
        });
        return result.toString();
    }

}
