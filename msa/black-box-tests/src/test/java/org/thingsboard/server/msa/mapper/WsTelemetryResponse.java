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
package org.thingsboard.server.msa.mapper;

import lombok.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class WsTelemetryResponse implements Serializable {
    private int subscriptionId;
    private int errorCode;
    private String errorMsg;
    private Map<String, List<List<Object>>> data;
    private Map<String, Object> latestValues;

    public List<Object> getDataValuesByKey(String key) {
        return data.entrySet().stream()
                .filter(e -> e.getKey().equals(key))
                .flatMap(e -> e.getValue().stream().flatMap(Collection::stream))
                .collect(Collectors.toList());
    }
}
